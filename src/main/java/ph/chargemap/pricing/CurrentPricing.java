package ph.chargemap.pricing;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Snapshot of a station's active price, embedded in the {@code Station} document for
 * fast reads. Superseded prices are appended to the {@code price_history} collection.
 * Price is PHP and stored with a currency-appropriate decimal type (Requirement 13.5).
 */
public class CurrentPricing {

    private BigDecimal pricePerKwh;
    private PricingModel pricingModel;
    private Instant effectiveFrom;
    private Instant effectiveTo;

    // Optional dynamic tariff: a cheaper off-peak rate applies outside the peak window.
    // When offPeakPricePerKwh is null there is no time-of-use pricing (flat rate only).
    private BigDecimal offPeakPricePerKwh;
    private Integer peakStartHour; // 0–23, local time
    private Integer peakEndHour;   // 0–23, local time (exclusive)

    public CurrentPricing() {
    }

    public CurrentPricing(BigDecimal pricePerKwh, PricingModel pricingModel,
                          Instant effectiveFrom, Instant effectiveTo) {
        this.pricePerKwh = pricePerKwh;
        this.pricingModel = pricingModel;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
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

    public BigDecimal getOffPeakPricePerKwh() {
        return offPeakPricePerKwh;
    }

    public void setOffPeakPricePerKwh(BigDecimal offPeakPricePerKwh) {
        this.offPeakPricePerKwh = offPeakPricePerKwh;
    }

    public Integer getPeakStartHour() {
        return peakStartHour;
    }

    public void setPeakStartHour(Integer peakStartHour) {
        this.peakStartHour = peakStartHour;
    }

    public Integer getPeakEndHour() {
        return peakEndHour;
    }

    public void setPeakEndHour(Integer peakEndHour) {
        this.peakEndHour = peakEndHour;
    }

    /** True when a distinct off-peak rate + peak window are configured. */
    public boolean hasDynamicTariff() {
        return offPeakPricePerKwh != null && peakStartHour != null && peakEndHour != null;
    }
}
