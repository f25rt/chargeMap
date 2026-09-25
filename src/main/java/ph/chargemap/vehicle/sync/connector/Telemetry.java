package ph.chargemap.vehicle.sync.connector;

import ph.chargemap.vehicle.sync.ChargingStatus;

/** Telemetry returned by a connector for a single vehicle at a point in time. */
public record Telemetry(
        int batteryPercentage,
        double rangeKm,
        ChargingStatus chargingStatus,
        double chargingSpeedKw,
        Double latitude,
        Double longitude,
        double odometerKm,
        Integer batteryHealthPercent
) {
}
