package ph.chargemap.telemetry;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ph.chargemap.telemetry.SimTelemetryDtos.GridTelemetryDto;
import ph.chargemap.telemetry.SimTelemetryDtos.StationTelemetryDto;

/**
 * SIMULATED telemetry endpoints for the HUD demo panels. All responses are flagged
 * {@code simulated = true}; there is no real charger-hardware feed behind these.
 *
 * <p>Station telemetry is public (matches the public {@code /api/stations/**} reads); the
 * grid telemetry lives under {@code /api/admin/**} so only admins see the Telemetry Hub feed.
 */
@RestController
public class SimTelemetryController {

    private final SimTelemetryService service;

    public SimTelemetryController(SimTelemetryService service) {
        this.service = service;
    }

    /** Simulated live bay telemetry for a station (public read). */
    @GetMapping("/api/stations/{id}/telemetry")
    public StationTelemetryDto stationTelemetry(@PathVariable String id) {
        return service.stationTelemetry(id);
    }

    /** Simulated grid-level telemetry + IoT stream (admin-only). */
    @GetMapping("/api/admin/telemetry/grid")
    public GridTelemetryDto gridTelemetry() {
        return service.gridTelemetry();
    }
}
