package ph.chargemap.station;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import ph.chargemap.availability.AvailabilitySummary;
import ph.chargemap.common.geo.GeoPoint;
import ph.chargemap.common.geo.GeoUtil;
import ph.chargemap.config.ChargeMapProperties;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.limit;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;
import static org.springframework.data.domain.Sort.Direction.ASC;

/**
 * Runs the geospatial aggregation pipelines that replace PostGIS queries (design.md):
 * nearby, cheapest-nearby, and available-nearby. All use a {@code $geoNear} stage over
 * the {@code 2dsphere} index on {@code stations.location}, which returns results sorted
 * by ascending distance and populates {@code distanceMeters}.
 */
@Service
public class StationGeoQueryService {

    private final MongoTemplate mongoTemplate;
    private final ChargeMapProperties props;
    private final AvailabilityStaleness staleness;

    public StationGeoQueryService(MongoTemplate mongoTemplate, ChargeMapProperties props) {
        this.mongoTemplate = mongoTemplate;
        this.props = props;
        this.staleness = new AvailabilityStaleness(props);
    }

    /** Requirement 2: stations within radius ordered by distance. */
    public List<GeoStation> nearby(GeoPoint center, double radiusKm, Criteria filter, int maxResults) {
        List<AggregationOperation> ops = new ArrayList<>();
        ops.add(geoNear(center, radiusKm, filter));
        ops.add(limit(maxResults));
        return run(ops);
    }

    /** Requirement 3: within radius, ordered by price then distance, excluding unpriced. */
    public List<GeoStation> cheapest(GeoPoint center, double radiusKm, Criteria filter, int maxResults) {
        List<AggregationOperation> ops = new ArrayList<>();
        ops.add(geoNear(center, radiusKm, filter));
        ops.add(match(Criteria.where("currentPricing.pricePerKwh").ne(null)));
        ops.add(sort(ASC, "currentPricing.pricePerKwh").and(ASC, "distanceMeters"));
        ops.add(limit(maxResults));
        return run(ops);
    }

    /** Requirement 4: within radius, with a fresh available charger, ordered by distance. */
    public List<GeoStation> available(GeoPoint center, double radiusKm, Criteria filter, int maxResults) {
        Instant cutoff = staleness.cutoff(Instant.now());
        List<AggregationOperation> ops = new ArrayList<>();
        ops.add(geoNear(center, radiusKm, filter));
        ops.add(match(new Criteria().andOperator(
                Criteria.where("availableCount").gt(0),
                Criteria.where("availabilitySummary").is(AvailabilitySummary.AVAILABLE.name()),
                Criteria.where("availabilityUpdatedAt").gte(cutoff)
        )));
        ops.add(limit(maxResults));
        return run(ops);
    }

    /**
     * Requirement 5: text search over indexed fields, broadened by charger-type/connector
     * keywords. MongoDB requires {@code $text} to be top-level (it cannot sit inside an
     * {@code $or} with other clauses), so the text match and the keyword match are run as
     * separate queries and merged, preserving text-score order first.
     */
    public List<Station> search(String query, int limit) {
        org.springframework.data.mongodb.core.query.TextCriteria text =
                org.springframework.data.mongodb.core.query.TextCriteria.forDefaultLanguage().matching(query);

        org.springframework.data.mongodb.core.query.TextQuery textQuery =
                org.springframework.data.mongodb.core.query.TextQuery.queryText(text).sortByScore();
        textQuery.limit(limit);
        List<Station> results = new ArrayList<>(mongoTemplate.find(textQuery, Station.class));

        List<Criteria> keywordBranches = new ArrayList<>();
        List<ph.chargemap.charger.ChargerType> types = SearchKeywords.matchChargerTypes(query);
        if (!types.isEmpty()) {
            keywordBranches.add(Criteria.where("chargers.chargerType")
                    .in(types.stream().map(Enum::name).toList()));
        }
        List<ph.chargemap.charger.ConnectorType> connectors = SearchKeywords.matchConnectors(query);
        if (!connectors.isEmpty()) {
            keywordBranches.add(Criteria.where("chargers.connectorType")
                    .in(connectors.stream().map(Enum::name).toList()));
        }

        if (!keywordBranches.isEmpty()) {
            var keywordQuery = new org.springframework.data.mongodb.core.query.Query(
                    new Criteria().orOperator(keywordBranches.toArray(new Criteria[0]))).limit(limit);
            java.util.Set<org.bson.types.ObjectId> seen = new java.util.HashSet<>();
            results.forEach(s -> seen.add(s.getId()));
            for (Station s : mongoTemplate.find(keywordQuery, Station.class)) {
                if (seen.add(s.getId()) && results.size() < limit) {
                    results.add(s);
                }
            }
        }
        return results;
    }

    /** Requirement 6 on the non-geo list: filtered, paginated station lookup. */
    public org.springframework.data.domain.Page<Station> listFiltered(
            Criteria filter, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.mongodb.core.query.Query query =
                (filter == null) ? new org.springframework.data.mongodb.core.query.Query()
                        : new org.springframework.data.mongodb.core.query.Query(filter);
        long total = mongoTemplate.count(query, Station.class);
        query.with(pageable);
        List<Station> content = mongoTemplate.find(query, Station.class);
        return new org.springframework.data.domain.PageImpl<>(content, pageable, total);
    }

    public Instant stalenessCutoff() {
        return staleness.cutoff(Instant.now());
    }

    public int maxResults() {
        return props.getGeo().getMaxResults();
    }

    public double defaultRadiusKm() {
        return props.getGeo().getDefaultRadiusKm();
    }

    private AggregationOperation geoNear(GeoPoint center, double radiusKm, Criteria filter) {
        GeoJsonPoint near = GeoUtil.toGeoJson(center);
        double maxDistanceMeters = GeoUtil.kmToMeters(radiusKm);
        // Build the $geoNear stage manually so we can attach an optional query filter.
        return context -> {
            org.bson.Document geoNear = new org.bson.Document();
            geoNear.put("near", new org.bson.Document("type", "Point")
                    .append("coordinates", List.of(near.getX(), near.getY())));
            geoNear.put("distanceField", "distanceMeters");
            geoNear.put("maxDistance", maxDistanceMeters);
            geoNear.put("spherical", true);
            if (filter != null) {
                geoNear.put("query", filter.getCriteriaObject());
            }
            return new org.bson.Document("$geoNear", geoNear);
        };
    }

    private List<GeoStation> run(List<AggregationOperation> ops) {
        Aggregation aggregation = newAggregation(ops);
        // Read raw documents so the $geoNear-injected distanceMeters is available, then
        // convert the station body separately (GeoStation is not a mapped entity).
        AggregationResults<org.bson.Document> results =
                mongoTemplate.aggregate(aggregation, "stations", org.bson.Document.class);
        List<GeoStation> mapped = new ArrayList<>();
        for (org.bson.Document doc : results.getMappedResults()) {
            Double distance = doc.get("distanceMeters") == null
                    ? null
                    : ((Number) doc.get("distanceMeters")).doubleValue();
            Station station = mongoTemplate.getConverter().read(Station.class, doc);
            mapped.add(new GeoStation(station, distance));
        }
        return mapped;
    }

    /** Small helper mirroring AvailabilityService's threshold for the aggregation cutoff. */
    static final class AvailabilityStaleness {
        private final long minutes;

        AvailabilityStaleness(ChargeMapProperties props) {
            this.minutes = props.getAvailability().getStalenessMinutes();
        }

        Instant cutoff(Instant now) {
            return now.minusSeconds(minutes * 60);
        }
    }
}
