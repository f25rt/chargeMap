package ph.chargemap.social;

import java.time.Instant;

/** DTOs for station reviews + likes. */
public final class SocialDtos {

    private SocialDtos() {
    }

    public record ReviewDto(
            String id,
            String stationId,
            String userId,
            String authorName,
            String text,
            String imageId,
            boolean mine,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record LikeStatusDto(long likeCount, boolean likedByMe) {
    }

    public record LikeTrendPoint(String date, long likes, long unlikes, long net) {
    }
}
