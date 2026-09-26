package ph.chargemap.social;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** A single user's like on a station. Unique per (station, user) so likes can't stack. */
@Document(collection = "station_likes")
@CompoundIndex(name = "station_user", def = "{'stationId': 1, 'userId': 1}", unique = true)
public class StationLike {

    @Id
    private ObjectId id;

    @Indexed
    private ObjectId stationId;
    private ObjectId userId;
    private Instant createdAt;

    public StationLike() {
    }

    public StationLike(ObjectId stationId, ObjectId userId, Instant createdAt) {
        this.stationId = stationId;
        this.userId = userId;
        this.createdAt = createdAt;
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

    public ObjectId getUserId() {
        return userId;
    }

    public void setUserId(ObjectId userId) {
        this.userId = userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
