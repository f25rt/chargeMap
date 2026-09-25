package ph.chargemap.operator;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import ph.chargemap.pricing.PricingModel;

import java.math.BigDecimal;

/** Operator updates a station's current price (PHP per kWh). */
public record PricingUpdateRequest(
        @NotNull @PositiveOrZero BigDecimal pricePerKwh,
        PricingModel pricingModel
) {
}
