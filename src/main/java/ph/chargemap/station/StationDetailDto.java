package ph.chargemap.station;

import ph.chargemap.availability.AvailabilitySummary;
import ph.chargemap.charger.ChargerDto;
import ph.chargemap.common.geo.GeoPoint;
import ph.chargemap.pricing.PricingDto;

import java.time.Instant;
import java.util.List;

/**
 * Full station detail for {@code GET /api/stations/{id}} (Requirement 1.2). Includes
 * chargers, current pricing, availability (staleness applied), operating hours,
 * amenities, rating, and data-quality metadata.
 */
public record StationDetailDto(
        String id,
        String name,
        String operator,
        String address,
        String area,
        GeoPoint location,
        String openingHours,
        String phone,
        List<String> amenities,
        Double rating,
        List<ChargerDto> chargers,
        PricingDto currentPricing,
        AvailabilitySummary availabilitySummary,
        int availableCount,
        int totalChargers,
        Instant availabilityUpdatedAt,
        DataSource dataSource,
        Confidence confidence,
        Instant lastVerified,
        Instant lastUpdated,
        boolean disabled,
        String imageId,
        long likeCount
) {
}
