package ph.chargemap.branch;

import java.time.Instant;

/** Branch view for management screens. Image ids are streamed via /api/images/{id}. */
public record BranchDto(
        String id,
        String name,
        String description,
        String area,
        String profileImageId,
        String bannerImageId,
        Instant createdAt
) {
    public static BranchDto from(Branch b) {
        return new BranchDto(
                b.getId().toHexString(),
                b.getName(),
                b.getDescription(),
                b.getArea(),
                b.getProfileImageId() == null ? null : b.getProfileImageId().toHexString(),
                b.getBannerImageId() == null ? null : b.getBannerImageId().toHexString(),
                b.getCreatedAt());
    }
}
