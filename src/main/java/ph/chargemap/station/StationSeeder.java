package ph.chargemap.station;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Component;
import ph.chargemap.availability.AvailabilitySummary;
import ph.chargemap.charger.Charger;
import ph.chargemap.charger.ChargerStatus;
import ph.chargemap.charger.ChargerType;
import ph.chargemap.charger.ConnectorType;
import ph.chargemap.pricing.CurrentPricing;
import ph.chargemap.pricing.PricingModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Seeds a demonstrable set of Metro Cebu charging stations on startup when the
 * collection is empty. Active only under the {@code dev} profile so tests and
 * production are unaffected (product spec section 30).
 */
@Component
@Profile("dev")
public class StationSeeder {

    private static final Logger log = LoggerFactory.getLogger(StationSeeder.class);

    /** Anchor points around Metro Cebu that seeded stations cluster near. */
    private static final double[][] ANCHORS = {
            {10.3181, 123.9068}, // Cebu Business Park / Ayala
            {10.3270, 123.9060}, // IT Park
            {10.3116, 123.9180}, // SM City Cebu (North Reclamation)
            {10.2447, 123.7896}, // SRP / SM Seaside
            {10.3103, 123.9494}, // Mactan / Lapu-Lapu
            {10.3520, 123.9310}, // Mandaue
    };

    private static final String[] AREAS = {
            "Cebu Business Park", "IT Park", "North Reclamation Area",
            "South Road Properties", "Lapu-Lapu", "Mandaue"
    };

    private static final String[] OPERATORS = {
            "Cebu EV Charge", "PowerUp PH", "GreenCharge", "VoltGo", "ChargeNow"
    };

    private final StationRepository repository;

    public StationSeeder(StationRepository repository) {
        this.repository = repository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(10) // after DemoDataReseeder (order 0)
    public void seed() {
        long existing = repository.count();
        if (existing > 0) {
            log.info("Station seed skipped; {} stations already present", existing);
            return;
        }
        List<Station> stations = new ArrayList<>();
        Random rnd = new Random(42); // deterministic seed for repeatable dev data
        int count = 10;
        for (int i = 0; i < count; i++) {
            stations.add(buildStation(i, rnd));
        }
        repository.saveAll(stations);
        log.info("Seeded {} Metro Cebu stations", stations.size());
    }

    private Station buildStation(int i, Random rnd) {
        int anchorIdx = i % ANCHORS.length;
        double[] anchor = ANCHORS[anchorIdx];
        // jitter up to ~0.02 deg (~2 km) around the anchor
        double lat = anchor[0] + (rnd.nextDouble() - 0.5) * 0.04;
        double lng = anchor[1] + (rnd.nextDouble() - 0.5) * 0.04;

        Instant now = Instant.now();

        Station s = new Station();
        s.setName(AREAS[anchorIdx] + " Charger #" + (i + 1));
        s.setOperator(OPERATORS[i % OPERATORS.length]);
        s.setAddress(AREAS[anchorIdx] + ", Metro Cebu");
        s.setArea(AREAS[anchorIdx]);
        s.setLocation(new GeoJsonPoint(lng, lat)); // GeoJSON [lng, lat]
        s.setOpeningHours(rnd.nextBoolean() ? "24 hours" : "6:00 AM - 10:00 PM");
        s.setPhone("+63 32 000 " + String.format("%04d", i));
        s.setAmenities(List.of("restroom", "cafe"));
        s.setRating(3.5 + rnd.nextDouble() * 1.5);

        List<Charger> chargers = buildChargers(rnd);
        s.setChargers(chargers);

        // Price varies ₱8–₱24/kWh
        BigDecimal price = BigDecimal.valueOf(8 + rnd.nextInt(17)).setScale(2);
        s.setCurrentPricing(new CurrentPricing(price, PricingModel.PER_KWH,
                now.minus(30, ChronoUnit.DAYS), null));

        int available = (int) chargers.stream().filter(c -> c.getStatus() == ChargerStatus.AVAILABLE).count();
        s.setTotalChargers(chargers.size());
        s.setAvailableCount(available);
        s.setAvailabilitySummary(available > 0 ? AvailabilitySummary.AVAILABLE
                : AvailabilitySummary.OCCUPIED);
        // Vary freshness: some recent, some stale (older than default 30 min threshold)
        s.setAvailabilityUpdatedAt(now.minus(rnd.nextInt(90), ChronoUnit.MINUTES));

        s.setDataSource(DataSource.values()[rnd.nextInt(DataSource.values().length)]);
        s.setConfidence(Confidence.values()[rnd.nextInt(Confidence.values().length)]);
        s.setLastVerified(s.getAvailabilityUpdatedAt());
        s.setCreatedAt(now.minus(60, ChronoUnit.DAYS));
        s.setLastUpdated(s.getAvailabilityUpdatedAt());
        return s;
    }

    private List<Charger> buildChargers(Random rnd) {
        int n = 2 + rnd.nextInt(5); // 2–6 chargers
        List<Charger> chargers = new ArrayList<>();
        ConnectorType[] connectors = ConnectorType.values();
        for (int j = 0; j < n; j++) {
            ConnectorType connector = connectors[rnd.nextInt(connectors.length)];
            ChargerType type;
            double power;
            int bucket = rnd.nextInt(4);
            switch (bucket) {
                case 0 -> { type = ChargerType.AC; power = 7 + rnd.nextInt(15); }        // <22
                case 1 -> { type = ChargerType.DC; power = 22 + rnd.nextInt(28); }       // 22-50
                case 2 -> { type = ChargerType.DC_FAST; power = 50 + rnd.nextInt(50); }  // 50-100
                default -> { type = ChargerType.DC_FAST; power = 100 + rnd.nextInt(150); } // 100+
            }
            ChargerStatus status = switch (rnd.nextInt(3)) {
                case 0 -> ChargerStatus.AVAILABLE;
                case 1 -> ChargerStatus.OCCUPIED;
                default -> ChargerStatus.UNKNOWN;
            };
            chargers.add(new Charger(new ObjectId(), connector, type, power, status, Instant.now()));
        }
        return chargers;
    }
}
