package ph.chargemap.pricing;

import java.math.BigDecimal;
import java.time.Instant;

/** Current pricing representation returned to clients (PHP). */
public record PricingDto(
        BigDecimal pricePerKwh,
        PricingModel pricingModel,
        Instant effectiveFrom,
        Instant effectiveTo
) {
    public static PricingDto from(CurrentPricing p) {
        if (p == null) {
            return null;
        }
        return new PricingDto(p.getPricePerKwh(), p.getPricingModel(),
                p.getEffectiveFrom(), p.getEffectiveTo());
    }
}
