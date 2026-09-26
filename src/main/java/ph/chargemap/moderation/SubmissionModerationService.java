package ph.chargemap.moderation;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ph.chargemap.availability.AvailabilitySummary;
import ph.chargemap.charger.Charger;
import ph.chargemap.charger.ChargerStatus;
import ph.chargemap.charger.ChargerType;
import ph.chargemap.charger.ConnectorType;
import ph.chargemap.common.error.BadRequestException;
import ph.chargemap.common.error.NotFoundException;
import ph.chargemap.pricing.CurrentPricing;
import ph.chargemap.pricing.PricingModel;
import ph.chargemap.points.PointsService;
import ph.chargemap.points.PointsService.TaskType;
import ph.chargemap.station.Confidence;
import ph.chargemap.station.DataSource;
import ph.chargemap.station.Station;
import ph.chargemap.station.StationRepository;
import ph.chargemap.submission.StationSubmission;
import ph.chargemap.submission.StationSubmissionRepository;
import ph.chargemap.submission.SubmissionComment;
import ph.chargemap.submission.SubmissionDto;
import ph.chargemap.submission.SubmissionStatus;
import ph.chargemap.user.User;
import ph.chargemap.user.UserService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin/operator review of station submissions (Requirement 2). Approving a submission
 * creates a live station (with its uploaded image) and awards STATION_ADD points to the
 * submitter.
 */
@Service
public class SubmissionModerationService {

    private final StationSubmissionRepository submissionRepository;
    private final StationRepository stationRepository;
    private final UserService userService;
    private final PointsService pointsService;
    private final ph.chargemap.activity.ActivityService activityService;

    public SubmissionModerationService(StationSubmissionRepository submissionRepository,
                                       StationRepository stationRepository,
                                       UserService userService,
                                       PointsService pointsService,
                                       ph.chargemap.activity.ActivityService activityService) {
        this.submissionRepository = submissionRepository;
        this.stationRepository = stationRepository;
        this.userService = userService;
        this.pointsService = pointsService;
        this.activityService = activityService;
    }

    public List<SubmissionDto> list(SubmissionStatus status) {
        List<StationSubmission> subs = (status == null)
                ? submissionRepository.findAllByOrderByCreatedAtDesc()
                : submissionRepository.findByStatusOrderByCreatedAtDesc(status);
        return subs.stream().map(SubmissionDto::from).toList();
    }

    public SubmissionDto get(String id) {
        return SubmissionDto.from(load(id));
    }

    public SubmissionDto comment(String id, String text) {
        StationSubmission s = load(id);
        User actor = userService.requireCurrentUser();
        s.getComments().add(new SubmissionComment(actor.getId(), actor.getName(),
                actor.getRole().name(), text, Instant.now()));
        s.setUpdatedAt(Instant.now());
        return SubmissionDto.from(submissionRepository.save(s));
    }

    /**
     * Approving a submission writes across multiple collections (station + submission +
     * points ledger + user balance). {@code @Transactional} makes those writes atomic:
     * they all commit together or all roll back, so we never leave an approved submission
     * without its points award (or vice versa).
     */
    @Transactional
    public SubmissionDto approve(String id) {
        StationSubmission s = load(id);
        if (s.getStatus() != SubmissionStatus.PENDING) {
            throw new BadRequestException("Submission is already " + s.getStatus());
        }
        if (s.getType() == ph.chargemap.submission.SubmissionType.EDIT) {
            return approveEdit(s);
        }
        User actor = userService.requireCurrentUser();
        Instant now = Instant.now();

        Station station = new Station();
        station.setName(s.getName());
        station.setOperator(s.getOperator());
        station.setAddress(s.getAddress());
        station.setArea(s.getArea() != null ? s.getArea() : s.getAddress());
        station.setLocation(s.getLocation());
        station.setImageId(s.getImageId());

        // Build one charger from the proposed details if provided.
        List<Charger> chargers = new ArrayList<>();
        if (s.getConnectorType() != null || s.getChargerType() != null || s.getPowerKw() != null) {
            Charger c = new Charger(
                    new ObjectId(),
                    s.getConnectorType() != null ? s.getConnectorType() : ConnectorType.CCS2,
                    s.getChargerType() != null ? s.getChargerType() : ChargerType.DC_FAST,
                    s.getPowerKw() != null ? s.getPowerKw() : 60,
                    ChargerStatus.UNKNOWN,
                    now);
            chargers.add(c);
        }
        station.setChargers(chargers);
        station.setTotalChargers(chargers.size());
        station.setAvailableCount(0);
        station.setAvailabilitySummary(AvailabilitySummary.UNKNOWN);
        station.setAvailabilityUpdatedAt(now);

        if (s.getProposedPricePerKwh() != null) {
            station.setCurrentPricing(new CurrentPricing(
                    s.getProposedPricePerKwh(), PricingModel.PER_KWH, now, null));
        }

        station.setDataSource(DataSource.USER_SUBMISSION);
        station.setConfidence(Confidence.LOW);
        station.setLastVerified(now);
        station.setCreatedAt(now);
        station.setLastUpdated(now);
        Station saved = stationRepository.save(station);

        s.setStatus(SubmissionStatus.APPROVED);
        s.setReviewedBy(actor.getId());
        s.setReviewedAt(now);
        s.setCreatedStationId(saved.getId());
        s.setUpdatedAt(now);
        submissionRepository.save(s);

        // Award points to the submitter for a successful contribution.
        pointsService.award(s.getSubmittedBy(), TaskType.STATION_ADD, saved.getId());

        return SubmissionDto.from(s);
    }

    /**
     * Applies an approved EDIT submission's non-null proposed fields to the target station,
     * then marks the submission APPROVED and awards STATION_UPDATE points. The live station
     * change becomes visible to all viewers on their next map refresh.
     */
    private SubmissionDto approveEdit(StationSubmission s) {
        User actor = userService.requireCurrentUser();
        Instant now = Instant.now();

        if (s.getTargetStationId() == null) {
            throw new BadRequestException("Edit submission has no target station");
        }
        Station station = stationRepository.findById(s.getTargetStationId())
                .orElseThrow(() -> new NotFoundException(
                        "Target station not found: " + s.getTargetStationId()));

        if (s.getName() != null) {
            station.setName(s.getName());
        }
        if (s.getOperator() != null) {
            station.setOperator(s.getOperator());
        }
        if (s.getAddress() != null) {
            station.setAddress(s.getAddress());
        }
        if (s.getArea() != null) {
            station.setArea(s.getArea());
        }
        if (s.getProposedPricePerKwh() != null) {
            station.setCurrentPricing(new CurrentPricing(
                    s.getProposedPricePerKwh(), PricingModel.PER_KWH, now, null));
        }
        // Apply connector/charger/power to the first charger, creating one if needed.
        if (s.getConnectorType() != null || s.getChargerType() != null || s.getPowerKw() != null) {
            List<Charger> chargers = station.getChargers() != null
                    ? new ArrayList<>(station.getChargers()) : new ArrayList<>();
            if (chargers.isEmpty()) {
                chargers.add(new Charger(new ObjectId(),
                        s.getConnectorType() != null ? s.getConnectorType() : ConnectorType.CCS2,
                        s.getChargerType() != null ? s.getChargerType() : ChargerType.DC_FAST,
                        s.getPowerKw() != null ? s.getPowerKw() : 60,
                        ChargerStatus.UNKNOWN, now));
            } else {
                Charger first = chargers.get(0);
                if (s.getConnectorType() != null) {
                    first.setConnectorType(s.getConnectorType());
                }
                if (s.getChargerType() != null) {
                    first.setChargerType(s.getChargerType());
                }
                if (s.getPowerKw() != null) {
                    first.setPowerKw(s.getPowerKw());
                }
            }
            station.setChargers(chargers);
            station.setTotalChargers(chargers.size());
        }
        station.setLastUpdated(now);
        Station saved = stationRepository.save(station);

        s.setStatus(SubmissionStatus.APPROVED);
        s.setReviewedBy(actor.getId());
        s.setReviewedAt(now);
        s.setCreatedStationId(saved.getId());
        s.setUpdatedAt(now);
        submissionRepository.save(s);

        pointsService.award(s.getSubmittedBy(), TaskType.STATION_UPDATE, saved.getId());
        // Advance any in-progress task activities for this contributor.
        activityService.recordApprovedStationUpdate(s.getSubmittedBy());
        return SubmissionDto.from(s);
    }

    public SubmissionDto reject(String id, String reason) {
        StationSubmission s = load(id);
        if (s.getStatus() != SubmissionStatus.PENDING) {
            throw new BadRequestException("Submission is already " + s.getStatus());
        }
        User actor = userService.requireCurrentUser();
        Instant now = Instant.now();
        s.getComments().add(new SubmissionComment(actor.getId(), actor.getName(),
                actor.getRole().name(), "Rejected: " + reason, now));
        s.setStatus(SubmissionStatus.REJECTED);
        s.setReviewedBy(actor.getId());
        s.setReviewedAt(now);
        s.setUpdatedAt(now);
        return SubmissionDto.from(submissionRepository.save(s));
    }

    private StationSubmission load(String id) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new BadRequestException("Invalid submission id: " + id);
        }
        return submissionRepository.findById(new ObjectId(id))
                .orElseThrow(() -> new NotFoundException("Submission not found: " + id));
    }
}
