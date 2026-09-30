package ph.chargemap.session;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

/** Request/response DTOs for charging sessions + user energy telemetry rollup. */
public final class SessionDtos {

    private SessionDtos() {
    }

    /** Body for logging a charging session. Only energyKwh is required. */
    public record LogSessionRequest(
            String stationId,
            String stationName,
            @NotNull @DecimalMin(value = "0.1", message = "Energy must be at least 0.1 kWh")
            Double energyKwh,
            Integer durationMinutes,
            Double peakKw,
            BigDecimal pricePerKwh,
            Instant startedAt
    ) {
    }

    public record SessionDto(
            String id,
            String stationId,
            String stationName,
            double energyKwh,
            Integer durationMinutes,
            Double peakKw,
            BigDecimal pricePerKwh,
            BigDecimal cost,
            double co2SavedKg,
            Instant startedAt
    ) {
        public static SessionDto from(ChargingSession s) {
            return new SessionDto(
                    s.getId() == null ? null : s.getId().toHexString(),
                    s.getStationId() == null ? null : s.getStationId().toHexString(),
                    s.getStationName(),
                    s.getEnergyKwh(),
                    s.getDurationMinutes(),
                    s.getPeakKw(),
                    s.getPricePerKwh(),
                    s.getCost(),
                    s.getCo2SavedKg(),
                    s.getStartedAt());
        }
    }

    /**
     * Aggregate driver telemetry rollup for the profile hero card. All values are derived
     * from the user's real logged sessions + contribution history.
     */
    public record TelemetryRollupDto(
            double energyLoggedKwh,
            double co2SavedKg,
            int sessionCount,
            BigDecimal totalSpent,
            Double avgPricePerKwh,
            int trustScorePercent,
            String trustTier
    ) {
    }
}
