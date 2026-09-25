package ph.chargemap.points;

import java.time.Instant;

/** A points ledger entry for the profile feed. */
public record PointsLedgerDto(
        PointsService.TaskType taskType,
        long points,
        String reason,
        Instant createdAt
) {
    public static PointsLedgerDto from(PointsLedgerEntry e) {
        return new PointsLedgerDto(e.getTaskType(), e.getPoints(), e.getReason(), e.getCreatedAt());
    }
}
