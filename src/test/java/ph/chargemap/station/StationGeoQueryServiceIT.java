package ph.chargemap.station;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import ph.chargemap.availability.AvailabilitySummary;
import ph.chargemap.charger.Charger;
import ph.chargemap.charger.ChargerStatus;
import ph.chargemap.charger.ChargerType;
import ph.chargemap.charger.ConnectorType;
import ph.chargemap.common.geo.GeoPoint;
import ph.chargemap.config.ChargeMapProperties;
import ph.chargemap.config.MongoIndexConfig;
import ph.chargemap.pricing.CurrentPricing;
import ph.chargemap.pricing.PricingModel;
import ph.chargemap.support.MongoTestContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the real $geoNear aggregation pipelines against MongoDB (Requirements 2, 3, 4).
 * Reference point: Cebu Business Park (10.3181, 123.9068).
 */
@DataMongoTest
class StationGeoQueryServiceIT extends MongoTestContainer {

    private static final GeoPoint CENTER = new GeoPoint(10.3181, 123.9068);

    @Autowired
    MongoTemplate mongoTemplate;

    StationGeoQueryService geoService;

    @BeforeEach
    void setUp() {
        mongoTemplate.remove(new org.springframework.data.mongodb.core.query.Query(), Station.class);
        ChargeMapProperties props = new ChargeMapProperties();
        props.getAvailability().setStalenessMinutes(30);
        props.getGeo().setMaxResults(50);
        geoService = new StationGeoQueryService(mongoTemplate, props);
        new MongoIndexConfig(mongoTemplate).ensureIndexes();
    }

    private Station station(String name, double lat, double lng, double price,
                            AvailabilitySummary summary, int availableCount, int staleMinutes) {
        Station s = new Station();
        s.setId(new ObjectId());
        s.setName(name);
        s.setLocation(new GeoJsonPoint(lng, lat));
        s.setCurrentPricing(price < 0 ? null
                : new CurrentPricing(BigDecimal.valueOf(price), PricingModel.PER_KWH,
                Instant.now().minus(10, ChronoUnit.DAYS), null));
        s.setChargers(List.of(new Charger(new ObjectId(), ConnectorType.CCS2, ChargerType.DC_FAST,
                60, ChargerStatus.AVAILABLE, Instant.now())));
        s.setAvailabilitySummary(summary);
        s.setAvailableCount(availableCount);
        s.setTotalChargers(1);
        s.setAvailabilityUpdatedAt(Instant.now().minus(staleMinutes, ChronoUnit.MINUTES));
        return s;
    }

    @Test
    void nearby_returnsWithinRadiusOrderedByDistance() {
        // ~0.5 km, ~2 km, ~30 km away
        Station near = station("Near", 10.3181, 123.9113, 15, AvailabilitySummary.AVAILABLE, 1, 5);
        Station mid = station("Mid", 10.3350, 123.9068, 15, AvailabilitySummary.AVAILABLE, 1, 5);
        Station far = station("Far", 10.5800, 123.9068, 15, AvailabilitySummary.AVAILABLE, 1, 5);
        mongoTemplate.insertAll(List.of(near, mid, far));

        List<GeoStation> results = geoService.nearby(CENTER, 5, null, 50);

        assertThat(results).extracting(GeoStation::getName).containsExactly("Near", "Mid");
        assertThat(results.get(0).getDistanceMeters()).isLessThan(results.get(1).getDistanceMeters());
    }

    @Test
    void cheapest_ordersByPriceThenDistanceAndExcludesUnpriced() {
        Station cheapFar = station("CheapFar", 10.3350, 123.9068, 10, AvailabilitySummary.AVAILABLE, 1, 5);
        Station pricyNear = station("PricyNear", 10.3181, 123.9113, 20, AvailabilitySummary.AVAILABLE, 1, 5);
        Station unpriced = station("Unpriced", 10.3181, 123.9090, -1, AvailabilitySummary.AVAILABLE, 1, 5);
        mongoTemplate.insertAll(List.of(cheapFar, pricyNear, unpriced));

        List<GeoStation> results = geoService.cheapest(CENTER, 5, null, 50);

        assertThat(results).extracting(GeoStation::getName).containsExactly("CheapFar", "PricyNear");
        assertThat(results).extracting(GeoStation::getName).doesNotContain("Unpriced");
    }

    @Test
    void available_excludesStaleAndOccupied() {
        Station fresh = station("Fresh", 10.3181, 123.9113, 15, AvailabilitySummary.AVAILABLE, 2, 5);
        Station stale = station("Stale", 10.3181, 123.9090, 15, AvailabilitySummary.AVAILABLE, 2, 90);
        Station occupied = station("Occupied", 10.3181, 123.9075, 15, AvailabilitySummary.OCCUPIED, 0, 5);
        mongoTemplate.insertAll(List.of(fresh, stale, occupied));

        List<GeoStation> results = geoService.available(CENTER, 5, null, 50);

        assertThat(results).extracting(GeoStation::getName).containsExactly("Fresh");
    }
}
