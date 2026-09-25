package ph.chargemap.user.dto;

import ph.chargemap.user.ContributorLevel;
import ph.chargemap.user.Role;
import ph.chargemap.user.User;

import java.time.Instant;

/** Current user's profile for {@code GET /api/users/me}. */
public record UserProfileDto(
        String id,
        String email,
        String name,
        Role role,
        int vehicleCount,
        int favoriteCount,
        boolean suspended,
        long pointsBalance,
        long lifetimePoints,
        String level,
        long stationsAdded,
        long updatesMade,
        Instant createdAt
) {
    public static UserProfileDto from(User u) {
        return new UserProfileDto(
                u.getId().toHexString(),
                u.getEmail(),
                u.getName(),
                u.getRole(),
                u.getVehicles() == null ? 0 : u.getVehicles().size(),
                u.getFavoriteStationIds() == null ? 0 : u.getFavoriteStationIds().size(),
                u.isSuspended(),
                u.getPointsBalance(),
                u.getLifetimePoints(),
                ContributorLevel.forPoints(u.getLifetimePoints()),
                u.getStationsAdded(),
                u.getUpdatesMade(),
                u.getCreatedAt()
        );
    }
}
