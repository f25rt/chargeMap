package ph.chargemap.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.stereotype.Component;
import ph.chargemap.station.Station;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;

/**
 * Ensures the indexes required for the filter queries described in the design that are
 * not (or not conveniently) expressed as document-field annotations. Spring Data's
 * auto-index-creation handles the {@code 2dsphere}, text, and single-field annotated
 * indexes; this adds indexes on embedded charger fields used by filters (Requirement 6).
 */
@Component
public class MongoIndexConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexConfig.class);

    private final MongoTemplate mongoTemplate;

    public MongoIndexConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Station.class);

        // Geo (2dsphere) and text indexes are created explicitly here rather than relying
        // solely on annotation auto-creation, so the full index set is deterministic even
        // when the collection is dropped and recreated (e.g. across integration tests).
        ops.ensureIndex(new GeospatialIndex("location")
                .typed(GeoSpatialIndexType.GEO_2DSPHERE).named("location_2dsphere"));
        ops.ensureIndex(new TextIndexDefinition.TextIndexDefinitionBuilder()
                .onField("name", 3f)
                .onField("operator")
                .onField("address")
                .onField("area")
                .named("station_text")
                .build());

        ops.ensureIndex(new Index().on("chargers.connectorType", Sort.Direction.ASC).named("charger_connectorType"));
        ops.ensureIndex(new Index().on("chargers.chargerType", Sort.Direction.ASC).named("charger_chargerType"));
        ops.ensureIndex(new Index().on("chargers.powerKw", Sort.Direction.ASC).named("charger_powerKw"));
        ops.ensureIndex(new Index().on("currentPricing.pricePerKwh", Sort.Direction.ASC).named("currentPricing_price"));

        // Charging sessions: history lookups are per-user, newest first.
        mongoTemplate.indexOps(ph.chargemap.session.ChargingSession.class)
                .ensureIndex(new Index()
                        .on("userId", Sort.Direction.ASC)
                        .on("startedAt", Sort.Direction.DESC)
                        .named("session_user_started"));

        log.info("Ensured ChargeMap geo, text, filter, and session indexes");
    }
}
