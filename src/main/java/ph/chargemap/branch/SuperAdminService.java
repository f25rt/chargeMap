package ph.chargemap.branch;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import ph.chargemap.common.error.BadRequestException;
import ph.chargemap.common.error.NotFoundException;
import ph.chargemap.user.Role;
import ph.chargemap.user.User;
import ph.chargemap.user.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

/** Super-admin oversight of admins: list, reassign branch, disable/enable. */
@Service
public class SuperAdminService {

    private final UserRepository userRepository;

    public SuperAdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
