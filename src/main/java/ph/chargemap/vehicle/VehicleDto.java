package ph.chargemap.vehicle;

import ph.chargemap.charger.ConnectorType;

import java.time.Instant;

/** Vehicle representation returned to clients (Requirement 10). */
public record VehicleDto(
        String id,
        String make,
        String model,
        double batteryCapacityKwh,
        ConnectorType connectorType,
        Double maxAcKw,
        Double maxDcKw,
        Instant createdAt,
        Instant updatedAt
) {
    public static VehicleDto from(Vehicle v) {
        return new VehicleDto(
                v.getVehicleId() == null ? null : v.getVehicleId().toHexString(),
                v.getMake(),
                v.getModel(),
                v.getBatteryCapacityKwh(),
                v.getConnectorType(),
                v.getMaxAcKw(),
                v.getMaxDcKw(),
                v.getCreatedAt(),
                v.getUpdatedAt()
        );
    }
}
