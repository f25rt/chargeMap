package ph.chargemap.activity;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A task activity users can take on. Completing the goal grants the linked prize. Phase 1
 * goal type: {@code APPROVED_STATION_UPDATES} — N approved station price/image updates.
 */
@Document(collection = "activities")
public class Activity {

    public enum GoalType {
        /** N of the user's station edit submissions get approved. */
        APPROVED_STATION_UPDATES
    }

    @Id
    private ObjectId id;

    private String title;
    private String description;
    private GoalType goalType = GoalType.APPROVED_STATION_UPDATES;
    private int goalCount;
    /** Prize granted on completion. */
    private ObjectId rewardPrizeId;
    private boolean active = true;
    private ObjectId createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public GoalType getGoalType() {
        return goalType;
    }

    public void setGoalType(GoalType goalType) {
        this.goalType = goalType;
    }

    public int getGoalCount() {
        return goalCount;
    }

    public void setGoalCount(int goalCount) {
        this.goalCount = goalCount;
    }

    public ObjectId getRewardPrizeId() {
        return rewardPrizeId;
    }

    public void setRewardPrizeId(ObjectId rewardPrizeId) {
        this.rewardPrizeId = rewardPrizeId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public ObjectId getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(ObjectId createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
