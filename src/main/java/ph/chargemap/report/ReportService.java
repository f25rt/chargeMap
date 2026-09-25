package ph.chargemap.report;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import ph.chargemap.availability.AvailabilitySummary;
import ph.chargemap.charger.Charger;
import ph.chargemap.charger.ChargerStatus;
import ph.chargemap.common.error.BadRequestException;
import ph.chargemap.common.error.NotFoundException;
import ph.chargemap.station.Station;
import ph.chargemap.station.StationRepository;

import java.time.Instant;
import java.util.List;

/**
 * Records user availability reports and updates the affected charger and the station's
 * denormalized availability rollup (Requirement 8). A report may target a specific
 * charger or the whole station.
 */
@Service
public class ReportService {

    private final StationRepository stationRepository;
    private final AvailabilityReportRepository reportRepository;
    private final ph.chargemap.user.UserService userService;
    private final ph.chargemap.points.PointsService pointsService;

    public ReportService(StationRepository stationRepository,
                         AvailabilityReportRepository reportRepository,
                         ph.chargemap.user.UserService userService,
                         ph.chargemap.points.PointsService pointsService) {
        this.stationRepository = stationRepository;
        this.reportRepository = reportRepository;
        this.userService = userService;
        this.pointsService = pointsService;
    }

    public ReportResponse submit(String stationId, ReportRequest request) {
        // Blocks suspended users (Requirement 5.2); also resolves the current user id.
        ObjectId userId = userService.requireActiveUser().getId();
        Station station = loadStation(stationId);
        ObjectId chargerId = resolveChargerId(station, request.chargerId());

        Instant now = Instant.now();

        // Persist the report.
        AvailabilityReport report = reportRepository.save(
                new AvailabilityReport(station.getId(), chargerId, userId, request.status(), now));

        // Update the target charger (if specified) and recompute the station rollup.
        applyToStation(station, chargerId, request.status(), now);
        stationRepository.save(station);

        // Reward the contributor for a report (subject to the daily cap).
        pointsService.award(userId, ph.chargemap.points.PointsService.TaskType.REPORT,
                report.getId());

        return new ReportResponse(
                "Thanks! Your report has been recorded.",
                report.getId().toHexString(),
                station.getAvailabilitySummary(),
                station.getAvailableCount(),
                station.getTotalChargers(),
                station.getLastUpdated()
        );
    }

    private Station loadStation(String stationId) {
        if (stationId == null || !ObjectId.isValid(stationId)) {
            throw new BadRequestException("Invalid station id: " + stationId);
        }
        return stationRepository.findById(new ObjectId(stationId))
                .orElseThrow(() -> NotFoundException.station(stationId));
    }

    /**
     * Validates the optional charger id belongs to the station and returns it, or null
     * when the report targets the whole station.
     */
    private ObjectId resolveChargerId(Station station, String chargerIdRaw) {
        if (chargerIdRaw == null || chargerIdRaw.isBlank()) {
            return null;
        }
        if (!ObjectId.isValid(chargerIdRaw)) {
            throw new BadRequestException("Invalid charger id: " + chargerIdRaw);
        }
        ObjectId chargerId = new ObjectId(chargerIdRaw);
        boolean belongs = station.getChargers().stream()
                .anyMatch(c -> chargerId.equals(c.getChargerId()));
        if (!belongs) {
            throw new BadRequestException("Charger does not belong to station: " + chargerIdRaw);
        }
        return chargerId;
    }

    private void applyToStation(Station station, ObjectId chargerId, ChargerStatus status, Instant now) {
        List<Charger> chargers = station.getChargers();
        if (chargerId != null) {
            for (Charger c : chargers) {
                if (chargerId.equals(c.getChargerId())) {
                    c.setStatus(status);
                    c.setStatusUpdatedAt(now);
                }
            }
        }

        int available = (int) chargers.stream()
                .filter(c -> c.getStatus() == ChargerStatus.AVAILABLE)
                .count();
        station.setAvailableCount(available);
        station.setTotalChargers(chargers.size());
        station.setAvailabilitySummary(
                available > 0 ? AvailabilitySummary.AVAILABLE : AvailabilitySummary.OCCUPIED);
        station.setAvailabilityUpdatedAt(now);
        station.setLastUpdated(now);
        station.setLastVerified(now);
    }
}
