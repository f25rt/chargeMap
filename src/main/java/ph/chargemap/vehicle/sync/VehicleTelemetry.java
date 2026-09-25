package ph.chargemap.vehicle.sync;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** The latest telemetry snapshot for a connected vehicle (one document per vehicle). */
@Document(collection = "vehicle_telemetry")
public class VehicleTelemetry {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    private ObjectId vehicleId;

    private Integer batteryPercentage;
    private Double rangeKm;
    private ChargingStatus chargingStatus;
    private Double chargingSpeedKw;
    private Double latitude;
    private Double longitude;
    private Double odometerKm;
    private Integer batteryHealthPercent;
    private Instant lastUpdated;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public ObjectId getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(ObjectId vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Integer getBatteryPercentage() {
        return batteryPercentage;
    }

    public void setBatteryPercentage(Integer batteryPercentage) {
        this.batteryPercentage = batteryPercentage;
    }

    public Double getRangeKm() {
        return rangeKm;
    }

    public void setRangeKm(Double rangeKm) {
        this.rangeKm = rangeKm;
    }

    public ChargingStatus getChargingStatus() {
        return chargingStatus;
    }

    public void setChargingStatus(ChargingStatus chargingStatus) {
        this.chargingStatus = chargingStatus;
    }

    public Double getChargingSpeedKw() {
        return chargingSpeedKw;
    }

    public void setChargingSpeedKw(Double chargingSpeedKw) {
        this.chargingSpeedKw = chargingSpeedKw;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getOdometerKm() {
        return odometerKm;
    }

    public void setOdometerKm(Double odometerKm) {
        this.odometerKm = odometerKm;
    }

    public Integer getBatteryHealthPercent() {
        return batteryHealthPercent;
    }

    public void setBatteryHealthPercent(Integer batteryHealthPercent) {
        this.batteryHealthPercent = batteryHealthPercent;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
