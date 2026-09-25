package ph.chargemap.admin;

import java.math.BigDecimal;
import java.time.Instant;

/** A station ranked by how often its price changed (Requirement 4). */
public record PricingTrendItem(
        String stationId,
        String stationName,
        long priceChanges,
        BigDecimal currentPrice,
        Instant lastChangedAt
) {
}
