package ph.chargemap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the ChargeMap PH backend MVP.
 *
 * <p>A modular Spring Boot monolith backed by MongoDB (2dsphere geospatial queries)
 * and Redis. See {@code specs/chargemap-backend-mvp} for the full spec.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class ChargeMapApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChargeMapApplication.class, args);
    }
}
