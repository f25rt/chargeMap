package ph.chargemap.activity;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ph.chargemap.activity.ActivityDtos.ActivityCompletionStat;
import ph.chargemap.activity.ActivityDtos.ActivityDto;
import ph.chargemap.activity.ActivityDtos.CreateActivityRequest;
import ph.chargemap.activity.ActivityDtos.UserActivityDto;
import ph.chargemap.common.error.BadRequestException;
import ph.chargemap.common.error.NotFoundException;
import ph.chargemap.points.PointsService;
import ph.chargemap.prize.Prize;
import ph.chargemap.prize.PrizeRepository;
import ph.chargemap.user.UserService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Task activities: admins create them (goal + linked reward), users enroll ("choose"),
 * and approved station updates advance progress. When the goal is met the activity is
 * completed and the linked prize is granted (credited as points + recorded for metrics).
 */
@Service
public class ActivityService {

    private static final Logger log = LoggerFactory.getLogger(ActivityService.class);

    private final ActivityRepository activityRepository;
    private final UserActivityRepository userActivityRepository;
    private final PrizeRepository prizeRepository;
    private final PointsService pointsService;
    private final UserService userService;

    public ActivityService(ActivityRepository activityRepository,
                           UserActivityRepository userActivityRepository,
                           PrizeRepository prizeRepository, PointsService pointsService,
                           UserService userService) {
        this.activityRepository = activityRepository;
        this.userActivityRepository = userActivityRepository;
        this.prizeRepository = prizeRepository;
        this.pointsService = pointsService;
        this.userService = userService;
    }

    // ----- Admin management -----

    public ActivityDto create(CreateActivityRequest req) {
        if (req.title() == null || req.title().isBlank()) {
            throw new BadRequestException("Activity title is required");
        }
        if (req.goalCount() == null || req.goalCount() < 1) {
            throw new BadRequestException("Goal count must be at least 1");
        }
        ObjectId creator = userService.requireCurrentUser().getId();
        Instant now = Instant.now();
        Activity a = new Activity();
        a.setTitle(req.title().trim());
        a.setDescription(req.description());
        a.setGoalType(Activity.GoalType.APPROVED_STATION_UPDATES);
        a.setGoalCount(req.goalCount());
        a.setRewardPrizeId(req.rewardPrizeId() != null && ObjectId.isValid(req.rewardPrizeId())
                ? new ObjectId(req.rewardPrizeId()) : null);
        a.setActive(true);
        a.setCreatedBy(creator);
        a.setCreatedAt(now);
        a.setUpdatedAt(now);
        return ActivityDto.from(activityRepository.save(a));
    }

    public List<ActivityDto> listAll() {
        return activityRepository.findAll().stream().map(ActivityDto::from).toList();
    }

    // ----- User-facing -----

    /** Active activities annotated with the current user's progress. */
    public List<UserActivityDto> listForCurrentUser() {
        ObjectId userId = userService.requireCurrentUser().getId();
        List<Activity> activities = activityRepository.findByActiveTrueOrderByCreatedAtDesc();
        List<UserActivityDto> out = new ArrayList<>();
        for (Activity a : activities) {
            UserActivity ua = userActivityRepository
                    .findByUserIdAndActivityId(userId, a.getId()).orElse(null);
            out.add(new UserActivityDto(
                    a.getId().toHexString(),
                    a.getTitle(),
                    a.getDescription(),
                    a.getGoalCount(),
                    a.getRewardPrizeId() == null ? null : a.getRewardPrizeId().toHexString(),
                    ua == null ? "NOT_STARTED" : ua.getStatus().name(),
                    ua == null ? 0 : ua.getProgress(),
                    ua == null || ua.getGrantedPrizeId() == null ? null
                            : ua.getGrantedPrizeId().toHexString()));
        }
        return out;
    }

    /** Enroll the current user in an activity ("choose" it). Idempotent. */
    public UserActivityDto choose(String activityId) {
        ObjectId userId = userService.requireActiveUser().getId();
        Activity a = loadActivity(activityId);
        UserActivity ua = userActivityRepository.findByUserIdAndActivityId(userId, a.getId())
                .orElseGet(() -> {
                    UserActivity fresh = new UserActivity();
                    fresh.setUserId(userId);
                    fresh.setActivityId(a.getId());
                    fresh.setGoalCount(a.getGoalCount());
                    fresh.setStatus(UserActivity.Status.IN_PROGRESS);
                    fresh.setStartedAt(Instant.now());
                    return userActivityRepository.save(fresh);
                });
        return new UserActivityDto(
                a.getId().toHexString(), a.getTitle(), a.getDescription(), a.getGoalCount(),
                a.getRewardPrizeId() == null ? null : a.getRewardPrizeId().toHexString(),
                ua.getStatus().name(), ua.getProgress(),
                ua.getGrantedPrizeId() == null ? null : ua.getGrantedPrizeId().toHexString());
    }

    /**
     * Called when one of the user's station edits is approved. Advances every in-progress
     * APPROVED_STATION_UPDATES activity by one; completes + grants the reward when the goal
     * is reached. Best-effort: never blocks the approval flow.
     */
    @Transactional
    public void recordApprovedStationUpdate(ObjectId userId) {
        if (userId == null) {
            return;
        }
        List<UserActivity> inProgress =
                userActivityRepository.findByUserIdAndStatus(userId, UserActivity.Status.IN_PROGRESS);
        for (UserActivity ua : inProgress) {
            ua.setProgress(ua.getProgress() + 1);
            if (ua.getProgress() >= ua.getGoalCount()) {
                completeAndGrant(ua);
            }
            userActivityRepository.save(ua);
        }
    }

    private void completeAndGrant(UserActivity ua) {
        ua.setStatus(UserActivity.Status.COMPLETED);
        ua.setCompletedAt(Instant.now());
        Activity a = activityRepository.findById(ua.getActivityId()).orElse(null);
        if (a != null && a.getRewardPrizeId() != null) {
            Prize prize = prizeRepository.findById(a.getRewardPrizeId()).orElse(null);
            if (prize != null) {
                // Grant the reward: credit its point value and record which prize was earned.
                pointsService.adjust(ua.getUserId(), prize.getPointCost(),
                        "Activity reward: " + a.getTitle() + " (" + prize.getName() + ")");
                ua.setGrantedPrizeId(prize.getId());
                log.info("Granted prize {} to user {} for activity {}",
                        prize.getId(), ua.getUserId(), a.getId());
            }
        }
    }

    // ----- Metrics -----

    /** Completion counts per activity/reward (for the "most completed" study). */
    public List<ActivityCompletionStat> completionStats() {
        Map<ObjectId, Long> counts = new HashMap<>();
        for (UserActivity ua : userActivityRepository.findByStatus(UserActivity.Status.COMPLETED)) {
            counts.merge(ua.getActivityId(), 1L, Long::sum);
        }
        List<ActivityCompletionStat> out = new ArrayList<>();
        for (Activity a : activityRepository.findAll()) {
            out.add(new ActivityCompletionStat(
                    a.getId().toHexString(),
                    a.getTitle(),
                    a.getRewardPrizeId() == null ? null : a.getRewardPrizeId().toHexString(),
                    counts.getOrDefault(a.getId(), 0L)));
        }
        out.sort((x, y) -> Long.compare(y.completions(), x.completions()));
        return out;
    }

    private Activity loadActivity(String id) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new BadRequestException("Invalid activity id: " + id);
        }
        return activityRepository.findById(new ObjectId(id))
                .orElseThrow(() -> new NotFoundException("Activity not found: " + id));
    }
}
