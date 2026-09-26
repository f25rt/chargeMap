package ph.chargemap.activity;

import java.time.Instant;

/** DTOs for the activity API. */
public final class ActivityDtos {

    private ActivityDtos() {
    }

    public record ActivityDto(
            String id,
            String title,
            String description,
            Activity.GoalType goalType,
            int goalCount,
            String rewardPrizeId,
            boolean active,
            Instant createdAt
    ) {
        public static ActivityDto from(Activity a) {
            return new ActivityDto(
                    a.getId().toHexString(),
                    a.getTitle(),
                    a.getDescription(),
                    a.getGoalType(),
                    a.getGoalCount(),
                    a.getRewardPrizeId() == null ? null : a.getRewardPrizeId().toHexString(),
                    a.isActive(),
                    a.getCreatedAt());
        }
    }

    /** An activity plus the current user's progress on it (null if not enrolled). */
    public record UserActivityDto(
            String activityId,
            String title,
            String description,
            int goalCount,
            String rewardPrizeId,
            String status,          // NOT_STARTED | IN_PROGRESS | COMPLETED
            int progress,
            String grantedPrizeId
    ) {
    }

    public record CreateActivityRequest(
            String title,
            String description,
            Integer goalCount,
            String rewardPrizeId
    ) {
    }

    /** For the "most-completed reward/activity" metric. */
    public record ActivityCompletionStat(
            String activityId,
            String title,
            String rewardPrizeId,
            long completions
    ) {
    }
}
