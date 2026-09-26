package ph.chargemap.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Seeds predictable demo accounts on startup so the app has ready-to-use logins.
 * Active only under the {@code dev} profile; production is unaffected. Accounts are
 * created only when missing, so passwords are never reset on restart.
 *
 * <p>Demo credentials (dev only):
 * <ul>
 *   <li>superadmin@chargemap.ph / password123  (SUPER_ADMIN)</li>
 *   <li>admin@chargemap.ph / password123  (ADMIN)</li>
 *   <li>user@chargemap.ph  / password123  (USER)</li>
 *   <li>maria@chargemap.ph / password123  (USER)</li>
 *   <li>operator@chargemap.ph / password123 (OPERATOR)</li>
 * </ul>
 */
@Component
@Profile("dev")
public class UserSeeder {

    private static final Logger log = LoggerFactory.getLogger(UserSeeder.class);
    private static final String DEFAULT_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(10) // after DemoDataReseeder (order 0)
    public void seed() {
        int created = 0;
        created += ensureUser("superadmin@chargemap.ph", "Super Admin", Role.SUPER_ADMIN);
        created += ensureUser("admin@chargemap.ph", "Admin", Role.ADMIN);
        created += ensureUser("user@chargemap.ph", "Demo User", Role.USER);
        created += ensureUser("maria@chargemap.ph", "Maria Santos", Role.USER);
        created += ensureUser("operator@chargemap.ph", "Demo Operator", Role.OPERATOR);
        if (created > 0) {
            log.info("Seeded {} demo account(s). Login password: {}", created, DEFAULT_PASSWORD);
        } else {
            log.info("Demo accounts already present; user seed skipped");
        }
    }

    private int ensureUser(String email, String name, Role role) {
        String normalized = email.toLowerCase();
        if (userRepository.existsByEmail(normalized)) {
            return 0;
        }
        User user = new User();
        user.setEmail(normalized);
        user.setName(name);
        user.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setRole(role);
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);
        return 1;
    }
}
