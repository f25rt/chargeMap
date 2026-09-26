package ph.chargemap.activity;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** A user's enrollment + progress on an {@link Activity}. */
@Document(collection = "user_activities")
public class UserActivity {

    public enum Status {IN_PROGRESS, COMPLETED}

    @Id
    private ObjectId id;

    @Indexed
    private ObjectId userId;
    private ObjectId activityId;
    private int progress;
    private int goalCount;      // snapshot of the activity goal at enroll time
    private Status status = Status.IN_PROGRESS;
    private ObjectId grantedPrizeId;
    private Instant startedAt;
    private Instant completedAt;

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

    public ObjectId getActivityId() {
        return activityId;
    }

    public void setActivityId(ObjectId activityId) {
        this.activityId = activityId;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getGoalCount() {
        return goalCount;
    }

    public void setGoalCount(int goalCount) {
        this.goalCount = goalCount;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public ObjectId getGrantedPrizeId() {
        return grantedPrizeId;
    }

    public void setGrantedPrizeId(ObjectId grantedPrizeId) {
        this.grantedPrizeId = grantedPrizeId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
