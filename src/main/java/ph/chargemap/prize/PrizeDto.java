package ph.chargemap.prize;

/** Prize as shown in the catalog. */
public record PrizeDto(
        String id,
        String name,
        String description,
        String imageId,
        int pointCost,
        boolean active
) {
    public static PrizeDto from(Prize p) {
        return new PrizeDto(
                p.getId().toHexString(),
                p.getName(),
                p.getDescription(),
                p.getImageId() == null ? null : p.getImageId().toHexString(),
                p.getPointCost(),
                p.isActive()
        );
    }
}
