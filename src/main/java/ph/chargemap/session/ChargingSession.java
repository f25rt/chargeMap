package ph.chargemap.session;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A charging session logged by a user (real, user-entered data). Powers the driver's
 * energy-logged / CO2-offset telemetry and session history. Cost and CO2 are derived and
 * stored denormalized so history reads don't recompute.
 */
@Document(collection = "charging_sessions")
@CompoundIndex(name = "user_started", def = "{'userId': 1, 'startedAt': -1}")
public class ChargingSession {

    @Id
    private ObjectId id;

    private ObjectId userId;

    /** Optional link to a station in our catalog. */
    private ObjectId stationId;
    /** Denormalized station name at time of logging (stations can be renamed/removed). */
    private String stationName;

    /** Energy delivered in kWh (required). */
    private double energyKwh;
    /** Session duration in minutes (optional). */
    private Integer durationMinutes;
    /** Peak delivery rate in kW (optional). */
    private Double peakKw;
    /** Price per kWh paid, PHP (optional). */
    private BigDecimal pricePerKwh;
    /** Total cost, PHP — derived from energy * price when both present. */
    private BigDecimal cost;
    /** CO2 avoided vs. an equivalent ICE trip, kg — derived from energyKwh. */
    private double co2SavedKg;

    private Instant startedAt;
    private Instant createdAt;

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

    public ObjectId getStationId() {
        return stationId;
    }

    public void setStationId(ObjectId stationId) {
        this.stationId = stationId;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public double getEnergyKwh() {
        return energyKwh;
    }

    public void setEnergyKwh(double energyKwh) {
        this.energyKwh = energyKwh;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Double getPeakKw() {
        return peakKw;
    }

    public void setPeakKw(Double peakKw) {
        this.peakKw = peakKw;
    }

    public BigDecimal getPricePerKwh() {
        return pricePerKwh;
    }

    public void setPricePerKwh(BigDecimal pricePerKwh) {
        this.pricePerKwh = pricePerKwh;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public double getCo2SavedKg() {
        return co2SavedKg;
    }

    public void setCo2SavedKg(double co2SavedKg) {
        this.co2SavedKg = co2SavedKg;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
