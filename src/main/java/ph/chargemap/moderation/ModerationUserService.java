package ph.chargemap.moderation;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import ph.chargemap.common.error.BadRequestException;
import ph.chargemap.common.error.ForbiddenException;
import ph.chargemap.common.error.NotFoundException;
import ph.chargemap.user.Role;
import ph.chargemap.user.User;
import ph.chargemap.user.UserRepository;
import ph.chargemap.user.UserService;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Admin/operator user management: list users, suspend and unsuspend (Requirement 5).
 * An OPERATOR may not suspend an ADMIN.
 */
@Service
public class ModerationUserService {

    private final UserRepository userRepository;
    private final UserService userService;

    public ModerationUserService(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    public List<ManagedUserDto> list() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(ManagedUserDto::from)
                .toList();
    }

    public ManagedUserDto suspend(String userId, String reason) {
        User actor = userService.requireCurrentUser();
        User target = load(userId);

        if (target.getId().equals(actor.getId())) {
            throw new BadRequestException("You cannot suspend your own account.");
        }
        if (target.getRole() == Role.ADMIN && actor.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Operators cannot suspend admin accounts.");
        }

        target.setSuspended(true);
        target.setSuspendedReason(reason);
        target.setSuspendedAt(Instant.now());
        target.setSuspendedBy(actor.getId());
        target.setUpdatedAt(Instant.now());
        return ManagedUserDto.from(userRepository.save(target));
    }

    public ManagedUserDto unsuspend(String userId) {
        User target = load(userId);
        target.setSuspended(false);
        target.setSuspendedReason(null);
        target.setSuspendedAt(null);
        target.setSuspendedBy(null);
        target.setUpdatedAt(Instant.now());
        return ManagedUserDto.from(userRepository.save(target));
    }

    private User load(String userId) {
        if (userId == null || !ObjectId.isValid(userId)) {
            throw new BadRequestException("Invalid user id: " + userId);
        }
        return userRepository.findById(new ObjectId(userId))
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }
}
