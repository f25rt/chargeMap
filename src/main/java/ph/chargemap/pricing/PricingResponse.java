package ph.chargemap.pricing;

import java.util.List;

/** Response for {@code GET /api/stations/{id}/pricing}: current price + history. */
public record PricingResponse(
        PricingDto current,
        List<PriceHistoryDto> history
) {
}
