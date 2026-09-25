package ph.chargemap.pricing;

import java.math.BigDecimal;
import java.time.Instant;

/** A superseded price entry returned in the pricing sub-resource (Requirement 7.2). */
public record PriceHistoryDto(
        BigDecimal pricePerKwh,
        PricingModel pricingModel,
        Instant effectiveFrom,
        Instant effectiveTo
) {
    public static PriceHistoryDto from(PriceHistory h) {
        return new PriceHistoryDto(h.getPricePerKwh(), h.getPricingModel(),
                h.getEffectiveFrom(), h.getEffectiveTo());
    }
}
