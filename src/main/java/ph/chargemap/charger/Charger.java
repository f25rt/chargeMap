package ph.chargemap.charger;

import org.bson.types.ObjectId;

import java.time.Instant;

/**
 * A single charging point, embedded in a {@code Station} document.
 *
 * <p>Carries a stable {@code chargerId} so user reports can target a specific charger
 * (this preserves the identity a relational {@code charger.id} would have).
 */
public class Charger {

    private ObjectId chargerId;
    private ConnectorType connectorType;
    private ChargerType chargerType;
    private double powerKw;
    private ChargerStatus status;
    private Instant statusUpdatedAt;

    public Charger() {
    }

    public Charger(ObjectId chargerId, ConnectorType connectorType, ChargerType chargerType,
                   double powerKw, ChargerStatus status, Instant statusUpdatedAt) {
        this.chargerId = chargerId;
        this.connectorType = connectorType;
        this.chargerType = chargerType;
        this.powerKw = powerKw;
        this.status = status;
        this.statusUpdatedAt = statusUpdatedAt;
    }

    public ObjectId getChargerId() {
        return chargerId;
    }

    public void setChargerId(ObjectId chargerId) {
        this.chargerId = chargerId;
    }

    public ConnectorType getConnectorType() {
        return connectorType;
    }

    public void setConnectorType(ConnectorType connectorType) {
        this.connectorType = connectorType;
    }

    public ChargerType getChargerType() {
        return chargerType;
    }

    public void setChargerType(ChargerType chargerType) {
        this.chargerType = chargerType;
    }

    public double getPowerKw() {
        return powerKw;
    }

    public void setPowerKw(double powerKw) {
        this.powerKw = powerKw;
    }

    public ChargerStatus getStatus() {
        return status;
    }

    public void setStatus(ChargerStatus status) {
        this.status = status;
    }

    public Instant getStatusUpdatedAt() {
        return statusUpdatedAt;
    }

    public void setStatusUpdatedAt(Instant statusUpdatedAt) {
        this.statusUpdatedAt = statusUpdatedAt;
    }
}
