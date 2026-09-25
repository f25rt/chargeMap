package ph.chargemap.points;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Append-only record of a points award or adjustment (design.md). Used for the profile
 * feed and for daily-cap counting.
 */
@Document(collection = "points_ledger")
@CompoundIndex(name = "user_created", def = "{'userId': 1, 'createdAt': -1}")
public class PointsLedgerEntry {

    @Id
    private ObjectId id;

    private ObjectId userId;
    private PointsService.TaskType taskType;
    private long points;
    private ObjectId refId;
    private String reason;
    private Instant createdAt;

    public PointsLedgerEntry() {
    }

    public PointsLedgerEntry(ObjectId userId, PointsService.TaskType taskType, long points,
                             ObjectId refId, String reason, Instant createdAt) {
        this.userId = userId;
        this.taskType = taskType;
        this.points = points;
        this.refId = refId;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public ObjectId getUserId() {
        return userId;
    }

    public void setUserId(ObjectId userId) {
        this.userId = userId;
    }

    public PointsService.TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(PointsService.TaskType taskType) {
        this.taskType = taskType;
    }

    public long getPoints() {
        return points;
    }

    public void setPoints(long points) {
        this.points = points;
    }

    public ObjectId getRefId() {
        return refId;
    }

    public void setRefId(ObjectId refId) {
        this.refId = refId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
