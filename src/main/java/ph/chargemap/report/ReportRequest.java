package ph.chargemap.report;

import jakarta.validation.constraints.NotNull;
import ph.chargemap.charger.ChargerStatus;

/**
 * Body for {@code POST /api/stations/{id}/reports} (Requirement 8). {@code chargerId} is
 * optional (a report may cover the whole station); {@code status} is required.
 */
public record ReportRequest(
        @NotNull ChargerStatus status,
        String chargerId
) {
}
