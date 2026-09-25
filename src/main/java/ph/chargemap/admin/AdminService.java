package ph.chargemap.admin;

import org.bson.types.ObjectId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ph.chargemap.availability.AvailabilityService;
import ph.chargemap.common.error.BadRequestException;
import ph.chargemap.common.error.NotFoundException;
import ph.chargemap.report.AvailabilityReport;
import ph.chargemap.report.AvailabilityReportRepository;
import ph.chargemap.station.Confidence;
import ph.chargemap.station.Station;
import ph.chargemap.station.StationDetailDto;
import ph.chargemap.station.StationMapper;
import ph.chargemap.station.StationRepository;
import ph.chargemap.user.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin oversight: system metrics, a recent-reports moderation feed, and station
 * moderation (verify / disable) — product spec section 38.
 */
@Service
public class AdminService {

    private final StationRepository stationRepository;
    private final UserRepository userRepository;
    private final AvailabilityReportRepository reportRepository;
    private final AvailabilityService availabilityService;
    private final StationMapper mapper;
    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    public AdminService(StationRepository stationRepository, UserRepository userRepository,
                        AvailabilityReportRepository reportRepository,
                        AvailabilityService availabilityService, StationMapper mapper,
                        org.springframework.data.mongodb.core.MongoTemplate mongoTemplate) {
        this.stationRepository = stationRepository;
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.availabilityService = availabilityService;
        this.mapper = mapper;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Stations ranked by number of price changes within the window (Requirement 4).
     * Counts entries in {@code price_history} per station since {@code now - days}.
     */
    public List<PricingTrendItem> pricingTrends(int days) {
        Instant since = Instant.now().minus(java.time.Duration.ofDays(days));

        var match = org.springframework.data.mongodb.core.aggregation.Aggregation.match(
                org.springframework.data.mongodb.core.query.Criteria.where("createdAt").gte(since));
        var group = org.springframework.data.mongodb.core.aggregation.Aggregation
                .group("stationId")
                .count().as("changes")
                .max("createdAt").as("lastChangedAt");
        var sort = org.springframework.data.mongodb.core.aggregation.Aggregation.sort(
                org.springframework.data.domain.Sort.Direction.DESC, "changes");
        var agg = org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(
                match, group, sort);

        var results = mongoTemplate.aggregate(agg, "price_history", org.bson.Document.class)
                .getMappedResults();

        // Enrich with station name + current price.
        var stationIds = results.stream()
                .map(d -> d.getObjectId("_id"))
                .filter(java.util.Objects::nonNull)
                .toList();
        var stationsById = stationRepository.findAllById(stationIds).stream()
                .collect(java.util.stream.Collectors.toMap(Station::getId, s -> s));

        java.util.List<PricingTrendItem> items = new java.util.ArrayList<>();
        for (org.bson.Document d : results) {
            ObjectId sid = d.getObjectId("_id");
            if (sid == null) continue;
            Station s = stationsById.get(sid);
            long changes = ((Number) d.get("changes")).longValue();
            java.util.Date last = d.getDate("lastChangedAt");
            items.add(new PricingTrendItem(
                    sid.toHexString(),
                    s != null ? s.getName() : "(unknown)",
                    changes,
                    s != null && s.getCurrentPricing() != null
                            ? s.getCurrentPricing().getPricePerKwh() : null,
                    last != null ? last.toInstant() : null));
        }
        return items;
    }

    public AdminStatsDto stats() {
        List<Station> stations = stationRepository.findAll();
        Instant now = Instant.now();
        long chargers = stations.stream().mapToLong(s -> s.getChargers().size()).sum();
        long available = stations.stream().mapToLong(Station::getAvailableCount).sum();
        long stale = stations.stream().filter(s -> availabilityService.isStale(s, now)).count();
        return new AdminStatsDto(
                stations.size(),
                chargers,
                available,
                userRepository.count(),
                reportRepository.count(),
                stale);
    }

    /** Most recent reports enriched with station names for the moderation feed. */
    public List<ReportFeedItem> recentReports(int limit) {
        List<AvailabilityReport> reports = reportRepository.findAll(
                PageRequest.of(0, Math.min(limit, 200), Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        Map<ObjectId, String> names = stationRepository
                .findAllById(reports.stream().map(AvailabilityReport::getStationId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Station::getId, Station::getName));

        return reports.stream()
                .map(r -> new ReportFeedItem(
                        r.getId().toHexString(),
                        r.getStationId().toHexString(),
                        names.getOrDefault(r.getStationId(), "(unknown station)"),
                        r.getChargerId() == null ? null : r.getChargerId().toHexString(),
                        r.getStatus(),
                        r.getCreatedAt()))
                .toList();
    }

    public List<StationDetailDto> allStations() {
        return stationRepository.findAll().stream().map(mapper::toDetail).toList();
    }

    /** Enable/disable a station (hides it from consumer results when disabled). */
    public StationDetailDto setDisabled(String stationId, boolean disabled) {
        Station station = load(stationId);
        station.setDisabled(disabled);
        station.setLastUpdated(Instant.now());
        stationRepository.save(station);
        return mapper.toDetail(station);
    }

    /** Mark a station verified: HIGH confidence + fresh lastVerified timestamp. */
    public StationDetailDto verify(String stationId) {
        Station station = load(stationId);
        station.setConfidence(Confidence.HIGH);
        station.setLastVerified(Instant.now());
        stationRepository.save(station);
        return mapper.toDetail(station);
    }

    private Station load(String stationId) {
        if (stationId == null || !ObjectId.isValid(stationId)) {
            throw new BadRequestException("Invalid station id: " + stationId);
        }
        return stationRepository.findById(new ObjectId(stationId))
                .orElseThrow(() -> NotFoundException.station(stationId));
    }
}
