package ph.chargemap.social;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Append-only like/unlike event for building the per-station like trend over time. */
@Document(collection = "station_like_events")
public class StationLikeEvent {

    @Id
    private ObjectId id;

    @Indexed
    private ObjectId stationId;
    private ObjectId branchId; // denormalized for branch-scoped trend queries
    private int delta;         // +1 like, -1 unlike
    @Indexed
    private Instant at;

    public StationLikeEvent() {
    }

    public StationLikeEvent(ObjectId stationId, ObjectId branchId, int delta, Instant at) {
        this.stationId = stationId;
        this.branchId = branchId;
        this.delta = delta;
        this.at = at;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public ObjectId getStationId() {
        return stationId;
    }

    public void setStationId(ObjectId stationId) {
        this.stationId = stationId;
    }

    public ObjectId getBranchId() {
        return branchId;
    }

    public void setBranchId(ObjectId branchId) {
        this.branchId = branchId;
    }

    public int getDelta() {
        return delta;
    }

    public void setDelta(int delta) {
        this.delta = delta;
    }

    public Instant getAt() {
        return at;
    }

    public void setAt(Instant at) {
        this.at = at;
    }
}
