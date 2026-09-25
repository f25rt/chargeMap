package ph.chargemap.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Dev-only, opt-in data reset. When {@code CHARGEMAP_RESEED=true}, clears the demo
 * collections on startup BEFORE the seeders run (guaranteed by the lowest {@code @Order}),
 * so the station/prize seeders repopulate from scratch to their current target counts.
 *
 * <p>Not destructive by default: with the flag unset this component does nothing. Users
 * are cleared here too, but {@code UserSeeder} recreates the demo accounts immediately.
 */
@Component
@Profile("dev")
public class DemoDataReseeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataReseeder.class);

    private final MongoTemplate mongoTemplate;
    private final boolean enabled;

    public DemoDataReseeder(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        this.enabled = Boolean.parseBoolean(System.getenv().getOrDefault("CHARGEMAP_RESEED", "false"));
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(0) // run before the seeders (default order = LOWEST_PRECEDENCE)
    public void reseed() {
        if (!enabled) {
            return;
        }
        String[] collections = {
                "stations", "prizes", "users", "station_submissions",
                "points_ledger", "reports", "favorites"
        };
        for (String c : collections) {
            if (mongoTemplate.collectionExists(c)) {
                long n = mongoTemplate.getCollection(c).countDocuments();
                mongoTemplate.getCollection(c).drop();
                log.warn("CHARGEMAP_RESEED: dropped collection '{}' ({} docs)", c, n);
            }
        }
        log.warn("CHARGEMAP_RESEED complete. Seeders will now repopulate demo data.");
    }
}
