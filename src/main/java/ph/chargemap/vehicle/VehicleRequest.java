package ph.chargemap.vehicle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import ph.chargemap.charger.ConnectorType;

/** Body for creating/updating a vehicle (Requirement 10). */
public record VehicleRequest(
        @NotBlank String make,
        @NotBlank String model,
        @Positive double batteryCapacityKwh,
        @NotNull ConnectorType connectorType,
        @PositiveOrZero Double maxAcKw,
        @PositiveOrZero Double maxDcKw
) {
}
