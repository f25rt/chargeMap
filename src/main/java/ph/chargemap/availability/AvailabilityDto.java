package ph.chargemap.availability;

import ph.chargemap.charger.ChargerDto;

import java.time.Instant;
import java.util.List;

/**
 * Response for {@code GET /api/stations/{id}/availability}: the effective station
 * summary (staleness applied), per-charger status, counts, and the last-updated
 * timestamp (Requirements 4.2, 7.3). Availability is never presented as real-time.
 */
public record AvailabilityDto(
        AvailabilitySummary summary,
        int availableCount,
        int totalChargers,
        Instant availabilityUpdatedAt,
        boolean stale,
        List<ChargerDto> chargers
) {
}
