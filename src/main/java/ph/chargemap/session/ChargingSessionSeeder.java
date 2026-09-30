package ph.chargemap.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ph.chargemap.station.Station;
import ph.chargemap.station.StationRepository;
import ph.chargemap.user.User;
import ph.chargemap.user.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Seeds a few charging sessions for the demo USER account so the profile telemetry
 * (energy logged, CO2 saved, session history) is populated out of the box. Dev only.
 */
@Component
@Profile("dev")
public class ChargingSessionSeeder {

    private static final Logger log = LoggerFactory.getLogger(ChargingSessionSeeder.class);
    private static final double CO2_KG_PER_KWH = 0.5;

    private final ChargingSessionRepository repository;
    private final UserRepository userRepository;
    private final StationRepository stationRepository;

    public ChargingSessionSeeder(ChargingSessionRepository repository,
                                 UserRepository userRepository,
                                 StationRepository stationRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(20) // after users + stations are seeded
    public void seed() {
        long existing = repository.count();
        if (existing > 0) {
            log.info("Charging session seed skipped; {} already present", existing);
            return;
        }
        User demo = userRepository.findByEmail("user@chargemap.ph").orElse(null);
        if (demo == null) {
            return;
        }
        List<Station> stations = stationRepository.findAll();
        Random rnd = new Random(7);
        List<ChargingSession> sessions = new ArrayList<>();
        Instant now = Instant.now();

        int count = 6;
        for (int i = 0; i < count; i++) {
            Station st = stations.isEmpty() ? null : stations.get(rnd.nextInt(stations.size()));
            double energy = round1(8 + rnd.nextDouble() * 42); // 8–50 kWh
            BigDecimal price = BigDecimal.valueOf(11 + rnd.nextInt(9)).setScale(2); // ₱11–19
            ChargingSession s = new ChargingSession();
            s.setUserId(demo.getId());
            if (st != null) {
                s.setStationId(st.getId());
                s.setStationName(st.getName());
            } else {
                s.setStationName("Unlinked session");
            }
            s.setEnergyKwh(energy);
            s.setDurationMinutes(15 + rnd.nextInt(60));
            s.setPeakKw((double) (20 + rnd.nextInt(130)));
            s.setPricePerKwh(price);
            s.setCost(price.multiply(BigDecimal.valueOf(energy)).setScale(2, RoundingMode.HALF_UP));
            s.setCo2SavedKg(round1(energy * CO2_KG_PER_KWH));
            s.setStartedAt(now.minus(i * 3L + 1, ChronoUnit.DAYS));
            s.setCreatedAt(now);
            sessions.add(s);
        }
        repository.saveAll(sessions);
        log.info("Seeded {} charging sessions for demo user", sessions.size());
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
