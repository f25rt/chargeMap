package ph.chargemap.vehicle;

import org.bson.types.ObjectId;
import ph.chargemap.charger.ConnectorType;

import java.time.Instant;

/**
 * An EV profile embedded under a user (Requirement 10). Carries a stable
 * {@code vehicleId} so individual vehicles can be updated or deleted.
 */
public class Vehicle {

    private ObjectId vehicleId;
    private String make;
    private String model;
    private double batteryCapacityKwh;
    private ConnectorType connectorType;
    private Double maxAcKw;
    private Double maxDcKw;
    private Instant createdAt;
    private Instant updatedAt;

    public Vehicle() {
    }

    public ObjectId getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(ObjectId vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getBatteryCapacityKwh() {
        return batteryCapacityKwh;
    }

    public void setBatteryCapacityKwh(double batteryCapacityKwh) {
        this.batteryCapacityKwh = batteryCapacityKwh;
    }

    public ConnectorType getConnectorType() {
        return connectorType;
    }

    public void setConnectorType(ConnectorType connectorType) {
        this.connectorType = connectorType;
    }

    public Double getMaxAcKw() {
        return maxAcKw;
    }

    public void setMaxAcKw(Double maxAcKw) {
        this.maxAcKw = maxAcKw;
    }

    public Double getMaxDcKw() {
        return maxDcKw;
    }

    public void setMaxDcKw(Double maxDcKw) {
        this.maxDcKw = maxDcKw;
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
