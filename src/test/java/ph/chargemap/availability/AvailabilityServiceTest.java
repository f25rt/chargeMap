package ph.chargemap.availability;

import org.junit.jupiter.api.Test;
import ph.chargemap.config.ChargeMapProperties;
import ph.chargemap.station.Station;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AvailabilityServiceTest {

    private AvailabilityService service() {
        ChargeMapProperties props = new ChargeMapProperties();
        props.getAvailability().setStalenessMinutes(30);
        return new AvailabilityService(props);
    }

    @Test
    void freshAvailability_isReturnedAsStored() {
        AvailabilityService service = service();
        Instant now = Instant.now();
        Station s = new Station();
        s.setAvailabilitySummary(AvailabilitySummary.AVAILABLE);
        s.setAvailabilityUpdatedAt(now.minus(10, ChronoUnit.MINUTES));

        assertThat(service.isStale(s, now)).isFalse();
        assertThat(service.effectiveSummary(s, now)).isEqualTo(AvailabilitySummary.AVAILABLE);
    }

    @Test
    void staleAvailability_isDowngradedToUnknown() {
        AvailabilityService service = service();
        Instant now = Instant.now();
        Station s = new Station();
        s.setAvailabilitySummary(AvailabilitySummary.AVAILABLE);
        s.setAvailabilityUpdatedAt(now.minus(45, ChronoUnit.MINUTES));

        assertThat(service.isStale(s, now)).isTrue();
        assertThat(service.effectiveSummary(s, now)).isEqualTo(AvailabilitySummary.UNKNOWN);
    }

    @Test
    void missingTimestamp_isTreatedAsStale() {
        AvailabilityService service = service();
        Station s = new Station();
        s.setAvailabilitySummary(AvailabilitySummary.AVAILABLE);
        s.setAvailabilityUpdatedAt(null);

        assertThat(service.isStale(s, Instant.now())).isTrue();
        assertThat(service.effectiveSummary(s, Instant.now())).isEqualTo(AvailabilitySummary.UNKNOWN);
    }
}
