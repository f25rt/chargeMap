package ph.chargemap.operator;

import jakarta.validation.constraints.NotNull;
import ph.chargemap.charger.ChargerStatus;

/** Operator updates a single charger's status. */
public record ChargerStatusUpdateRequest(@NotNull ChargerStatus status) {
}
