package ph.chargemap.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Ensures exactly one SUPER_ADMIN exists on startup. Runs in ALL profiles (not just dev)
 * so a fresh production database is bootstrapped with a single super-admin account — and
 * nothing else (stations, prizes, and other users stay empty because their seeders are
 * dev-only).
 *
 * <p>Credentials come from env:
 * <ul>
 *   <li>{@code CHARGEMAP_SUPERADMIN_EMAIL} (default {@code superadmin@chargemap.ph})</li>
 *   <li>{@code CHARGEMAP_SUPERADMIN_PASSWORD} — required in production; a dev fallback is
 *       used only when unset (logged as a warning).</li>
 * </ul>
 *
 * <p>Idempotent + single-instance: if a SUPER_ADMIN already exists, it does nothing, so
 * there is never more than one seeded super admin.
 */
@Component
public class SuperAdminBootstrap {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminBootstrap.class);
    private static final String DEFAULT_EMAIL = "superadmin@chargemap.ph";
    private static final String DEV_FALLBACK_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(5) // after DemoDataReseeder (0), before the dev seeders (10)
    public void ensureSuperAdmin() {
        // Exactly one: if any SUPER_ADMIN exists, do nothing.
        if (!userRepository.findByRole(Role.SUPER_ADMIN).isEmpty()) {
            log.info("Super admin already present; bootstrap skipped");
            return;
        }

        String email = envOr("CHARGEMAP_SUPERADMIN_EMAIL", DEFAULT_EMAIL).toLowerCase();
        String password = System.getenv("CHARGEMAP_SUPERADMIN_PASSWORD");
        if (password == null || password.isBlank()) {
            log.warn("CHARGEMAP_SUPERADMIN_PASSWORD not set — using a DEV-ONLY default "
                    + "password. Set a real password in production.");
            password = DEV_FALLBACK_PASSWORD;
        }

        // If a user with that email exists but isn't a super admin (shouldn't normally
        // happen), don't clobber it — just log and skip.
        if (userRepository.existsByEmail(email)) {
            log.warn("A user with email {} already exists; skipping super-admin bootstrap", email);
            return;
        }

        User u = new User();
        u.setEmail(email);
        u.setName(envOr("CHARGEMAP_SUPERADMIN_NAME", "Super Admin"));
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setRole(Role.SUPER_ADMIN);
        Instant now = Instant.now();
        u.setCreatedAt(now);
        u.setUpdatedAt(now);
        userRepository.save(u);
        log.info("Bootstrapped the initial SUPER_ADMIN account: {}", email);
    }

    private String envOr(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
