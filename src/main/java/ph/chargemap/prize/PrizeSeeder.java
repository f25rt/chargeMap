package ph.chargemap.prize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Seeds a small demo prize catalog on startup when the collection is empty. Active only
 * under the {@code dev} profile so tests and production are unaffected.
 */
@Component
@Profile("dev")
public class PrizeSeeder {

    private static final Logger log = LoggerFactory.getLogger(PrizeSeeder.class);

    private record Seed(String name, String description, int pointCost) {
    }

    private static final List<Seed> PRIZES = List.of(
            new Seed("ChargeMap Sticker Pack", "A set of vinyl EV stickers.", 100),
            new Seed("Reusable Coffee Tumbler", "Insulated tumbler for the road.", 300),
            new Seed("₱200 Charging Credit", "Credit toward your next charge.", 500),
            new Seed("ChargeMap T-Shirt", "Soft cotton tee with the ChargeMap logo.", 800),
            new Seed("Free Fast-Charge Session", "One complimentary DC fast-charge session.", 1500)
    );

    private final PrizeRepository repository;

    public PrizeSeeder(PrizeRepository repository) {
        this.repository = repository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(10) // after DemoDataReseeder (order 0)
    public void seed() {
        long existing = repository.count();
        if (existing > 0) {
            log.info("Prize seed skipped; {} prizes already present", existing);
            return;
        }
        Instant now = Instant.now();
        List<Prize> prizes = PRIZES.stream().map(s -> {
            Prize p = new Prize();
            p.setName(s.name());
            p.setDescription(s.description());
            p.setPointCost(s.pointCost());
            p.setActive(true);
            p.setCreatedAt(now);
            p.setUpdatedAt(now);
            return p;
        }).toList();
        repository.saveAll(prizes);
        log.info("Seeded {} demo prizes", prizes.size());
    }
}
