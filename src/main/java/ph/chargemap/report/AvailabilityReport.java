package ph.chargemap.report;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import ph.chargemap.charger.ChargerStatus;

import java.time.Instant;

/**
 * An append-only record of a user-reported charger status (Requirement 8). Kept in its
 * own collection because reports are high-volume and read as history.
 */
@Document(collection = "availability_reports")
@CompoundIndex(name = "station_created", def = "{'stationId': 1, 'createdAt': -1}")
public class AvailabilityReport {

    @Id
    private ObjectId id;

    private ObjectId stationId;
    private ObjectId chargerId;
    private ObjectId userId;
    private ChargerStatus status;
    private Instant createdAt;

    public AvailabilityReport() {
    }

    public AvailabilityReport(ObjectId stationId, ObjectId chargerId, ObjectId userId,
                              ChargerStatus status, Instant createdAt) {
        this.stationId = stationId;
        this.chargerId = chargerId;
        this.userId = userId;
        this.status = status;
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

    public ObjectId getChargerId() {
        return chargerId;
    }

    public void setChargerId(ObjectId chargerId) {
        this.chargerId = chargerId;
    }

    public ObjectId getUserId() {
        return userId;
    }

    public void setUserId(ObjectId userId) {
        this.userId = userId;
    }

    public ChargerStatus getStatus() {
        return status;
    }

    public void setStatus(ChargerStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
