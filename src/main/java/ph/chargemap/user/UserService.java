package ph.chargemap.user;

import org.bson.types.ObjectId;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ph.chargemap.common.error.ConflictException;
import ph.chargemap.common.error.NotFoundException;
import ph.chargemap.config.ChargeMapProperties;
import ph.chargemap.security.CurrentUser;
import ph.chargemap.security.JwtService;
import ph.chargemap.user.dto.LoginRequest;
import ph.chargemap.user.dto.RegisterUserRequest;
import ph.chargemap.user.dto.TokenResponse;
import ph.chargemap.user.dto.UserProfileDto;

import java.time.Instant;
import java.util.Locale;

/**
 * User registration, authentication, and profile retrieval (Requirement 9).
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long expiryMinutes;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, ChargeMapProperties props) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.expiryMinutes = props.getSecurity().getJwt().getExpiryMinutes();
    }

    public UserProfileDto register(RegisterUserRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already registered");
        }
        User user = new User();
        user.setEmail(email);
        user.setName(request.name());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return UserProfileDto.from(userRepository.save(user));
    }

    public TokenResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new CurrentUser.UnauthorizedException());
        String token = jwtService.issueToken(user);
        return TokenResponse.bearer(token, expiryMinutes * 60);
    }

    public UserProfileDto me() {
        return UserProfileDto.from(requireCurrentUser());
    }

    public User requireCurrentUser() {
        ObjectId id = CurrentUser.requireId();
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    /**
     * Returns the current user, or throws 403 if they are suspended. Contribution
     * endpoints (submit, report, favorites/vehicle writes) call this so suspended users
     * can still browse but cannot contribute (Requirement 5.2).
     */
    public User requireActiveUser() {
        User user = requireCurrentUser();
        if (user.isSuspended()) {
            throw new ph.chargemap.common.error.ForbiddenException(
                    "Your account is suspended and cannot contribute.");
        }
        return user;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
