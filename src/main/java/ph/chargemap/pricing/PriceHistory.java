package ph.chargemap.pricing;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A superseded price record for a station (product spec section 10). The active price
 * is embedded on the station; when it changes, the previous price is appended here to
 * support price history (Requirement 7.2) and the future 30-day chart.
 */
@Document(collection = "price_history")
public class PriceHistory {

    @Id
    private ObjectId id;

    @Indexed
    private ObjectId stationId;

    private BigDecimal pricePerKwh;
    private PricingModel pricingModel;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private Instant createdAt;

    public PriceHistory() {
    }

    public PriceHistory(ObjectId stationId, BigDecimal pricePerKwh, PricingModel pricingModel,
                        Instant effectiveFrom, Instant effectiveTo, Instant createdAt) {
        this.stationId = stationId;
        this.pricePerKwh = pricePerKwh;
        this.pricingModel = pricingModel;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
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

    public BigDecimal getPricePerKwh() {
        return pricePerKwh;
    }

    public void setPricePerKwh(BigDecimal pricePerKwh) {
        this.pricePerKwh = pricePerKwh;
    }

    public PricingModel getPricingModel() {
        return pricingModel;
    }

    public void setPricingModel(PricingModel pricingModel) {
        this.pricingModel = pricingModel;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Instant effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
