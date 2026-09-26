package ph.chargemap.branch;

import ph.chargemap.user.Role;
import ph.chargemap.user.User;

/** Admin overview row for super-admin management. */
public record AdminSummaryDto(
        String id,
        String name,
        String email,
        Role role,
        String branchId,
        boolean adminDisabled
) {
    public static AdminSummaryDto from(User u) {
        return new AdminSummaryDto(
                u.getId().toHexString(),
                u.getName(),
                u.getEmail(),
                u.getRole(),
                u.getBranchId() == null ? null : u.getBranchId().toHexString(),
                u.isAdminDisabled());
    }
}
