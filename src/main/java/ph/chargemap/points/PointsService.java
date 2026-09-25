package ph.chargemap.points;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import ph.chargemap.user.User;
import ph.chargemap.user.UserRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Awards contribution points with a points ledger, configurable rules, and per-task
 * daily caps (Requirement 6). Contributor level is derived from lifetime points.
 */
@Service
public class PointsService {

    public enum TaskType {
        STATION_ADD,
        STATION_UPDATE,
        REPORT,
        ADMIN_ADJUST
    }

    // Daily caps reset at local midnight (Asia/Manila for this deployment).
    private static final ZoneId ZONE = ZoneId.of("Asia/Manila");

    private final UserRepository userRepository;
    private final PointsLedgerRepository ledgerRepository;
    private final PointRulesService rulesService;

    public PointsService(UserRepository userRepository, PointsLedgerRepository ledgerRepository,
                         PointRulesService rulesService) {
        this.userRepository = userRepository;
        this.ledgerRepository = ledgerRepository;
        this.rulesService = rulesService;
    }

    /**
     * Awards points for a completed task, honoring the per-task daily cap. Records a
     * ledger entry and updates the user's balances/counters. Returns points actually
     * awarded (0 if capped or user missing).
     */
    public long award(ObjectId userId, TaskType taskType, ObjectId refId) {
        if (userId == null || taskType == TaskType.ADMIN_ADJUST) {
            return 0;
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return 0;
        }
        PointRules rules = rulesService.get();
        int base = pointsFor(rules, taskType);
        int cap = dailyCap(rules, taskType);

        long awardedToday = awardedToday(userId, taskType);
        long remaining = Math.max(0, cap - awardedToday);
        long grant = Math.min(base, remaining);
        if (grant <= 0) {
            return 0;
        }

        Instant now = Instant.now();
        ledgerRepository.save(new PointsLedgerEntry(userId, taskType, grant, refId, null, now));

        user.setPointsBalance(user.getPointsBalance() + grant);
        user.setLifetimePoints(user.getLifetimePoints() + grant);
        if (taskType == TaskType.STATION_ADD) {
            user.setStationsAdded(user.getStationsAdded() + 1);
        } else if (taskType == TaskType.STATION_UPDATE || taskType == TaskType.REPORT) {
            user.setUpdatesMade(user.getUpdatesMade() + 1);
        }
        user.setUpdatedAt(now);
        userRepository.save(user);
        return grant;
    }

    /** Admin/operator manual adjustment (may be negative). Always recorded in the ledger. */
    public long adjust(ObjectId userId, long delta, String reason) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return 0;
        }
        Instant now = Instant.now();
        ledgerRepository.save(new PointsLedgerEntry(userId, TaskType.ADMIN_ADJUST, delta, null,
                reason, now));
        user.setPointsBalance(Math.max(0, user.getPointsBalance() + delta));
        if (delta > 0) {
            user.setLifetimePoints(user.getLifetimePoints() + delta);
        }
        user.setUpdatedAt(now);
        userRepository.save(user);
        return delta;
    }

    public List<PointsLedgerEntry> recentLedger(ObjectId userId) {
        return ledgerRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId);
    }

    private long awardedToday(ObjectId userId, TaskType taskType) {
        Instant midnight = LocalDate.now(ZONE).atStartOfDay(ZONE).toInstant();
        return ledgerRepository
                .findByUserIdAndTaskTypeAndCreatedAtGreaterThanEqual(userId, taskType, midnight)
                .stream()
                .mapToLong(PointsLedgerEntry::getPoints)
                .sum();
    }

    private int pointsFor(PointRules r, TaskType type) {
        return switch (type) {
            case STATION_ADD -> r.getPointsPerStationAdd();
            case STATION_UPDATE -> r.getPointsPerStationUpdate();
            case REPORT -> r.getPointsPerReport();
            case ADMIN_ADJUST -> 0;
        };
    }

    private int dailyCap(PointRules r, TaskType type) {
        return switch (type) {
            case STATION_ADD -> r.getDailyCapStationAdd();
            case STATION_UPDATE -> r.getDailyCapStationUpdate();
            case REPORT -> r.getDailyCapReport();
            case ADMIN_ADJUST -> Integer.MAX_VALUE;
        };
    }
}
