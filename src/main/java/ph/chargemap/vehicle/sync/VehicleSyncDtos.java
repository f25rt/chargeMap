package ph.chargemap.vehicle.sync;

import java.time.Instant;

/** Request/response DTOs for the vehicle-sync API. Tokens and VIN are never exposed. */
public final class VehicleSyncDtos {

    private VehicleSyncDtos() {
    }

    /** Body for connecting a vehicle. In Phase 1, {@code authCode} is optional (mock). */
    public record ConnectRequest(
            Manufacturer manufacturer,
            String nickname,
            Boolean locationConsent,
            Integer chargeTargetPercent,
            String authCode
    ) {
    }

    /** Battery status tier derived from battery percentage. */
    public enum BatteryTier {EXCELLENT, GOOD, LOW, CRITICAL, UNKNOWN}

    public record TelemetryDto(
            Integer batteryPercentage,
            BatteryTier batteryTier,
            Double rangeKm,
            ChargingStatus chargingStatus,
            Double chargingSpeedKw,
            Integer batteryHealthPercent,
            Double odometerKm,
            Instant lastUpdated
    ) {
    }

    public record ConnectedVehicleDto(
            String id,
            Manufacturer manufacturer,
            String model,
            Integer year,
            String nickname,
            boolean connected,
            boolean locationConsent,
            int chargeTargetPercent,
            Instant lastSyncAt,
            TelemetryDto telemetry
    ) {
    }

    public record LocationDto(Double latitude, Double longitude, Instant lastUpdate) {
    }

    public record SyncResponse(boolean success, Instant lastSync, int vehiclesSynced) {
    }

    /** Battery tier from percentage (80+ Excellent, 50+ Good, 20+ Low, else Critical). */
    public static BatteryTier tierOf(Integer battery) {
        if (battery == null) {
            return BatteryTier.UNKNOWN;
        }
        if (battery >= 80) {
            return BatteryTier.EXCELLENT;
        }
        if (battery >= 50) {
            return BatteryTier.GOOD;
        }
        if (battery >= 20) {
            return BatteryTier.LOW;
        }
        return BatteryTier.CRITICAL;
    }
}
