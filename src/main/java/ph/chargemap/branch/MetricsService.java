package ph.chargemap.branch;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import ph.chargemap.station.Station;
import ph.chargemap.station.StationRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Studies/metrics: branch price-change trend over a window, and per-station update
 * frequency (approved user edits + price changes). Reads {@code price_history} and
 * {@code station_submissions}.
 */
@Service
public class MetricsService {

    private final MongoTemplate mongoTemplate;
    private final StationRepository stationRepository;
    private final BranchRepository branchRepository;

    public MetricsService(MongoTemplate mongoTemplate, StationRepository stationRepository,
                          BranchRepository branchRepository) {
        this.mongoTemplate = mongoTemplate;
        this.stationRepository = stationRepository;
        this.branchRepository = branchRepository;
    }

    /** Price-change count per branch within the last {@code days} (e.g. 1=day, 7=week). */
    public List<BranchPriceTrend> branchPriceTrend(int days) {
        Instant since = Instant.now().minus(Duration.ofDays(Math.max(1, days)));
        // Count price_history entries in window, grouped by station.
        var agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("createdAt").gte(since)),
                Aggregation.group("stationId").count().as("changes"));
        var perStation = mongoTemplate.aggregate(agg, "price_history", Document.class)
                .getMappedResults();

        // Map station -> branch, then roll station counts up to branch.
        Map<ObjectId, ObjectId> stationToBranch = stationRepository.findAll().stream()
                .filter(s -> s.getBranchId() != null)
                .collect(Collectors.toMap(Station::getId, Station::getBranchId));

        Map<ObjectId, Long> branchCounts = new java.util.HashMap<>();
        long unassigned = 0;
        for (Document d : perStation) {
            ObjectId sid = d.getObjectId("_id");
            long c = ((Number) d.get("changes")).longValue();
            ObjectId bid = sid == null ? null : stationToBranch.get(sid);
            if (bid == null) {
                unassigned += c;
            } else {
                branchCounts.merge(bid, c, Long::sum);
            }
        }

        List<BranchPriceTrend> out = new ArrayList<>();
        for (Branch b : branchRepository.findAll()) {
            out.add(new BranchPriceTrend(b.getId().toHexString(), b.getName(),
                    branchCounts.getOrDefault(b.getId(), 0L)));
        }
        if (unassigned > 0) {
            out.add(new BranchPriceTrend(null, "(unassigned stations)", unassigned));
        }
        out.sort((x, y) -> Long.compare(y.priceChanges(), x.priceChanges()));
        return out;
    }

    /**
     * For one station: how many approved user edit-submissions it has received (a proxy for
     * how often users update its prices/images) plus its recorded price-change count.
     */
    public StationUpdateFrequency stationUpdateFrequency(String stationId, int days) {
        if (stationId == null || !ObjectId.isValid(stationId)) {
            throw new ph.chargemap.common.error.BadRequestException("Invalid station id");
        }
        ObjectId sid = new ObjectId(stationId);
        Instant since = Instant.now().minus(Duration.ofDays(Math.max(1, days)));

        long approvedEdits = mongoTemplate.getCollection("station_submissions").countDocuments(
                new Document("targetStationId", sid)
                        .append("type", "EDIT")
                        .append("status", "APPROVED")
                        .append("reviewedAt", new Document("$gte", java.util.Date.from(since))));

        long priceChanges = mongoTemplate.getCollection("price_history").countDocuments(
                new Document("stationId", sid)
                        .append("createdAt", new Document("$gte", java.util.Date.from(since))));

        String name = stationRepository.findById(sid).map(Station::getName).orElse("(unknown)");
        return new StationUpdateFrequency(stationId, name, approvedEdits, priceChanges, days);
    }

    /**
     * Daily like trend. Scoped to {@code branchId} when provided (the calling admin's
     * branch); a null branchId means all branches (super admin). Returns one point per day
     * in the window with net likes (likes - unlikes).
     */
    public List<LikeTrendPoint> likeTrend(ObjectId branchId, int days) {
        Instant since = Instant.now().minus(Duration.ofDays(Math.max(1, days)));
        Criteria criteria = Criteria.where("at").gte(since);
        if (branchId != null) {
            criteria = criteria.and("branchId").is(branchId);
        }
        var agg = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.project()
                        .and(org.springframework.data.mongodb.core.aggregation.DateOperators.DateToString
                                .dateOf("at").toString("%Y-%m-%d")).as("day")
                        .and("delta").as("delta"),
                Aggregation.group("day")
                        .sum(org.springframework.data.mongodb.core.aggregation.ConditionalOperators
                                .when(Criteria.where("delta").gt(0)).then(1).otherwise(0)).as("likes")
                        .sum(org.springframework.data.mongodb.core.aggregation.ConditionalOperators
                                .when(Criteria.where("delta").lt(0)).then(1).otherwise(0)).as("unlikes")
                        .sum("delta").as("net"),
                Aggregation.sort(Sort.Direction.ASC, "_id"));
        var results = mongoTemplate.aggregate(agg, "station_like_events", Document.class)
                .getMappedResults();
        List<LikeTrendPoint> out = new ArrayList<>();
        for (Document d : results) {
            out.add(new LikeTrendPoint(
                    d.getString("_id"),
                    ((Number) d.getOrDefault("likes", 0)).longValue(),
                    ((Number) d.getOrDefault("unlikes", 0)).longValue(),
                    ((Number) d.getOrDefault("net", 0)).longValue()));
        }
        return out;
    }

    public record BranchPriceTrend(String branchId, String branchName, long priceChanges) {
    }

    public record StationUpdateFrequency(
            String stationId, String stationName, long approvedUpdates, long priceChanges, int windowDays) {
    }

    public record LikeTrendPoint(String date, long likes, long unlikes, long net) {
    }
}
