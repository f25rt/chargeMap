package ph.chargemap.availability;

import org.springframework.stereotype.Service;
import ph.chargemap.config.ChargeMapProperties;
import ph.chargemap.station.Station;

import java.time.Duration;
import java.time.Instant;

/**
 * Owns the availability staleness rule (Requirements 4.2, 4.3, 6.2): a station's stored
 * availability is only trusted while it is fresh. Once the stored
 * {@code availabilityUpdatedAt} is older than the configured threshold, the effective
 * summary is downgraded to {@link AvailabilitySummary#UNKNOWN} so the API never presents
 * stale data as available.
 */
@Service
public class AvailabilityService {

    private final Duration stalenessThreshold;

    public AvailabilityService(ChargeMapProperties props) {
        this.stalenessThreshold = Duration.ofMinutes(props.getAvailability().getStalenessMinutes());
    }

    /** Returns the availability summary to expose for a station at the given instant. */
    public AvailabilitySummary effectiveSummary(Station station, Instant now) {
        if (isStale(station, now)) {
            return AvailabilitySummary.UNKNOWN;
        }
        return station.getAvailabilitySummary() == null
                ? AvailabilitySummary.UNKNOWN
                : station.getAvailabilitySummary();
    }

    /** True when the station's availability information is older than the threshold. */
    public boolean isStale(Station station, Instant now) {
        Instant updatedAt = station.getAvailabilityUpdatedAt();
        if (updatedAt == null) {
            return true;
        }
        return updatedAt.isBefore(now.minus(stalenessThreshold));
    }

    public Duration stalenessThreshold() {
        return stalenessThreshold;
    }
}
