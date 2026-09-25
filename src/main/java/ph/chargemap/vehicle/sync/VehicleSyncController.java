package ph.chargemap.vehicle.sync;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ph.chargemap.vehicle.sync.VehicleSyncDtos.ConnectRequest;
import ph.chargemap.vehicle.sync.VehicleSyncDtos.ConnectedVehicleDto;
import ph.chargemap.vehicle.sync.VehicleSyncDtos.LocationDto;
import ph.chargemap.vehicle.sync.VehicleSyncDtos.SyncResponse;

import java.util.List;

/**
 * Vehicle Sync & Live Telemetry (Phase 1). All endpoints require authentication and only
 * ever act on the calling user's own connected vehicles. Tokens and VIN are never returned.
 */
@RestController
@RequestMapping("/api/vehicle")
public class VehicleSyncController {

    private final VehicleSyncService vehicleSyncService;

    public VehicleSyncController(VehicleSyncService vehicleSyncService) {
        this.vehicleSyncService = vehicleSyncService;
    }

    /** Connect a vehicle (used at sign-up and later from the profile). */
    @PostMapping("/connect")
    public ConnectedVehicleDto connect(@RequestBody ConnectRequest req) {
        return vehicleSyncService.connect(req);
    }

    /** List the caller's connected vehicles with their current telemetry. */
    @GetMapping
    public List<ConnectedVehicleDto> list() {
        return vehicleSyncService.listCurrentUser();
    }

    /** Sync all of the caller's vehicles on demand. */
    @PostMapping("/sync")
    public SyncResponse sync() {
        return vehicleSyncService.syncCurrentUser();
    }

    /** Battery/range/charging status for a vehicle (or the caller's primary one). */
    @GetMapping("/status")
    public ConnectedVehicleDto status(@RequestParam(required = false) String vehicleId) {
        return vehicleSyncService.status(vehicleId);
    }

    /** Latest known location for a vehicle (or the caller's primary one). */
    @GetMapping("/location")
    public LocationDto location(@RequestParam(required = false) String vehicleId) {
        return vehicleSyncService.location(vehicleId);
    }

    /** Disconnect a vehicle: deletes tokens + telemetry and stops syncing. */
    @DeleteMapping("/{id}")
    public void disconnect(@PathVariable String id) {
        vehicleSyncService.disconnect(id);
    }
}
