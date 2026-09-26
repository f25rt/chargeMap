package ph.chargemap.branch;

import org.bson.types.ObjectId;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ph.chargemap.common.error.BadRequestException;
import ph.chargemap.common.error.ConflictException;
import ph.chargemap.common.error.NotFoundException;
import ph.chargemap.user.Role;
import ph.chargemap.user.User;
import ph.chargemap.user.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

/** Super-admin oversight of admins: create, list, reassign branch, disable/enable. */
@Service
public class SuperAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Creates a new ADMIN account (super-admin only). Optionally assigns a branch. */
    public AdminSummaryDto createAdmin(String email, String name, String password, String branchId) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email is required");
        }
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required");
        }
        if (password == null || password.length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters");
        }
        String normalized = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalized)) {
            throw new ConflictException("Email already registered");
        }
        User u = new User();
        u.setEmail(normalized);
        u.setName(name.trim());
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setRole(Role.ADMIN);
        if (branchId != null && !branchId.isBlank()) {
            if (!ObjectId.isValid(branchId)) {
                throw new BadRequestException("Invalid branch id");
            }
            u.setBranchId(new ObjectId(branchId));
        }
        Instant now = Instant.now();
        u.setCreatedAt(now);
        u.setUpdatedAt(now);
        return AdminSummaryDto.from(userRepository.save(u));
    }

    /** All ADMIN + SUPER_ADMIN accounts. */
    public List<AdminSummaryDto> listAdmins() {
        return Stream.concat(
                        userRepository.findByRole(Role.ADMIN).stream(),
                        userRepository.findByRole(Role.SUPER_ADMIN).stream())
                .map(AdminSummaryDto::from)
                .toList();
    }

    public AdminSummaryDto reassignBranch(String adminId, String branchId) {
        User admin = loadAdmin(adminId);
        admin.setBranchId((branchId == null || branchId.isBlank()) ? null : new ObjectId(branchId));
        admin.setUpdatedAt(Instant.now());
        return AdminSummaryDto.from(userRepository.save(admin));
    }

    public AdminSummaryDto setDisabled(String adminId, boolean disabled) {
        User admin = loadAdmin(adminId);
        if (admin.getRole() == Role.SUPER_ADMIN) {
            throw new BadRequestException("A super admin cannot be disabled");
        }
        admin.setAdminDisabled(disabled);
        admin.setUpdatedAt(Instant.now());
        return AdminSummaryDto.from(userRepository.save(admin));
    }

    private User loadAdmin(String id) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new BadRequestException("Invalid admin id: " + id);
        }
        User u = userRepository.findById(new ObjectId(id))
                .orElseThrow(() -> new NotFoundException("Admin not found: " + id));
        if (u.getRole() != Role.ADMIN && u.getRole() != Role.SUPER_ADMIN) {
            throw new BadRequestException("User is not an admin");
        }
        return u;
    }
}
