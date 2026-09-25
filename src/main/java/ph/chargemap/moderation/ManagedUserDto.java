package ph.chargemap.moderation;

import ph.chargemap.user.ContributorLevel;
import ph.chargemap.user.Role;
import ph.chargemap.user.User;

import java.time.Instant;

/** User row for the admin/operator user-management table (Requirement 5.4). */
public record ManagedUserDto(
        String id,
        String email,
        String name,
        Role role,
        boolean suspended,
        String suspendedReason,
        long pointsBalance,
        long lifetimePoints,
        String level,
        long stationsAdded,
        long updatesMade,
        Instant createdAt
) {
    public static ManagedUserDto from(User u) {
        return new ManagedUserDto(
                u.getId().toHexString(),
                u.getEmail(),
                u.getName(),
                u.getRole(),
                u.isSuspended(),
                u.getSuspendedReason(),
                u.getPointsBalance(),
                u.getLifetimePoints(),
                ContributorLevel.forPoints(u.getLifetimePoints()),
                u.getStationsAdded(),
                u.getUpdatesMade(),
                u.getCreatedAt()
        );
    }
}
