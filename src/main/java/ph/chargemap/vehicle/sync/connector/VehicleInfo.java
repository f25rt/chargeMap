package ph.chargemap.vehicle.sync.connector;

/** Static vehicle identity returned by a connector's vehicle list. */
public record VehicleInfo(
        String providerVehicleId,
        String model,
        Integer year,
        String vin
) {
}
