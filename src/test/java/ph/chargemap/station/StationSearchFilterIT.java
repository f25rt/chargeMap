package ph.chargemap.station;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import ph.chargemap.availability.AvailabilitySummary;
import ph.chargemap.charger.Charger;
import ph.chargemap.charger.ChargerStatus;
import ph.chargemap.charger.ChargerType;
import ph.chargemap.charger.ConnectorType;
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
 * Exercises text search (with keyword mapping) and the filtered list against a real
 * MongoDB (Requirements 5, 6).
 */
@DataMongoTest
class StationSearchFilterIT extends MongoTestContainer {

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

        mongoTemplate.insertAll(List.of(
                station("SM City Cebu", "SM Prime", ConnectorType.CCS2, ChargerType.DC_FAST, 60, 15),
                station("Ayala Center Cebu", "Ayala Land", ConnectorType.TYPE2, ChargerType.AC, 7, 12),
                station("IT Park Hub", "PowerUp PH", ConnectorType.CHADEMO, ChargerType.DC, 40, 18)
        ));
    }

    private Station station(String name, String operator, ConnectorType connector,
                            ChargerType type, double kw, double price) {
        Station s = new Station();
        s.setId(new ObjectId());
        s.setName(name);
        s.setOperator(operator);
        s.setAddress(name + ", Cebu");
        s.setArea(name);
        s.setLocation(new GeoJsonPoint(123.9, 10.31));
        s.setChargers(List.of(new Charger(new ObjectId(), connector, type, kw,
                ChargerStatus.AVAILABLE, Instant.now())));
        s.setCurrentPricing(new CurrentPricing(BigDecimal.valueOf(price), PricingModel.PER_KWH,
                Instant.now().minus(5, ChronoUnit.DAYS), null));
        s.setAvailabilitySummary(AvailabilitySummary.AVAILABLE);
        s.setAvailableCount(1);
        s.setTotalChargers(1);
        s.setAvailabilityUpdatedAt(Instant.now().minus(5, ChronoUnit.MINUTES));
        return s;
    }

    @Test
    void search_matchesByName() {
        List<Station> results = geoService.search("Ayala", 50);
        assertThat(results).extracting(Station::getName).contains("Ayala Center Cebu");
    }

    @Test
    void search_keywordMapping_matchesChargerType() {
        // "DC fast" maps to ChargerType.DC_FAST even though no station name contains it.
        List<Station> results = geoService.search("DC fast", 50);
        assertThat(results).extracting(Station::getName).contains("SM City Cebu");
    }

    @Test
    void listFiltered_byConnector() {
        StationFilter filter = StationFilter.parse(null, "CHADEMO", null, null, null, null);
        var page = geoService.listFiltered(filter.toCriteria(geoService.stalenessCutoff()),
                PageRequest.of(0, 20));
        assertThat(page.getContent()).extracting(Station::getName).containsExactly("IT Park Hub");
    }

    @Test
    void listFiltered_byPriceMax() {
        StationFilter filter = StationFilter.parse(null, null, null, null, new BigDecimal("13"), null);
        var page = geoService.listFiltered(filter.toCriteria(geoService.stalenessCutoff()),
                PageRequest.of(0, 20));
        assertThat(page.getContent()).extracting(Station::getName).containsExactly("Ayala Center Cebu");
    }
}
