package ph.chargemap.operator;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import ph.chargemap.pricing.PricingModel;

import java.math.BigDecimal;

/**
 * Operator updates a station's price (PHP per kWh). Optional dynamic tariff: when a
 * cheaper {@code offPeakPricePerKwh} + a peak window ({@code peakStartHour}–{@code
 * peakEndHour}, 0–23) are provided, the station has time-of-use pricing.
 */
public record PricingUpdateRequest(
        @NotNull @PositiveOrZero BigDecimal pricePerKwh,
        PricingModel pricingModel,
        @PositiveOrZero BigDecimal offPeakPricePerKwh,
        Integer peakStartHour,
        Integer peakEndHour
) {
}
