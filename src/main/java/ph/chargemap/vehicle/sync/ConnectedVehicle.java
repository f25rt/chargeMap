package ph.chargemap.vehicle.sync;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A user's connected EV account for live telemetry sync. Distinct from the user's saved
 * {@code ph.chargemap.vehicle.Vehicle} profile (make/model/connector used for matching).
 * VIN is stored encrypted.
 */
@Document(collection = "connected_vehicles")
public class ConnectedVehicle {

    @Id
    private ObjectId id;

    @Indexed
    private ObjectId userId;

    private Manufacturer manufacturer;
    private String model;
    private Integer year;
    /** Encrypted VIN (never returned in DTOs). */
    private String vinEnc;
    private String nickname;
    private String providerVehicleId;

    private boolean locationConsent = true;
    private int chargeTargetPercent = 80;
    private boolean connected = true;

    private Instant lastSyncAt;
    private Instant createdAt;
    private Instant updatedAt;

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

    public Manufacturer getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(Manufacturer manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getVinEnc() {
        return vinEnc;
    }

    public void setVinEnc(String vinEnc) {
        this.vinEnc = vinEnc;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getProviderVehicleId() {
        return providerVehicleId;
    }

    public void setProviderVehicleId(String providerVehicleId) {
        this.providerVehicleId = providerVehicleId;
    }

    public boolean isLocationConsent() {
        return locationConsent;
    }

    public void setLocationConsent(boolean locationConsent) {
        this.locationConsent = locationConsent;
    }

    public int getChargeTargetPercent() {
        return chargeTargetPercent;
    }

    public void setChargeTargetPercent(int chargeTargetPercent) {
        this.chargeTargetPercent = chargeTargetPercent;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public Instant getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(Instant lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
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
