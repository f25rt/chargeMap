package ph.chargemap.station;

import ph.chargemap.availability.AvailabilitySummary;
import ph.chargemap.common.geo.GeoPoint;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Compact station representation for list and geo results. {@code distanceMeters} is
 * populated only for geo queries (Requirement 2.1); it is null otherwise.
 */
public record StationSummaryDto(
        String id,
        String name,
        String operator,
        String area,
        GeoPoint location,
        Double distanceMeters,
        BigDecimal pricePerKwh,
        int availableCount,
        int totalChargers,
        AvailabilitySummary availabilitySummary,
        Instant availabilityUpdatedAt,
        DataSource dataSource,
        Confidence confidence,
        Instant lastUpdated
) {
}
