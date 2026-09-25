package ph.chargemap.vehicle.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background telemetry sync. Every 5 minutes, refreshes all connected vehicles so the
 * dashboard stays current without user action (Requirement 4.2).
 */
@Component
public class VehicleSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(VehicleSyncScheduler.class);

    private final VehicleSyncService vehicleSyncService;

    public VehicleSyncScheduler(VehicleSyncService vehicleSyncService) {
        this.vehicleSyncService = vehicleSyncService;
    }

    // fixedDelay so a slow run never overlaps the next; initial delay lets startup settle.
    @Scheduled(initialDelay = 60_000, fixedDelay = 300_000)
    public void syncConnectedVehicles() {
        try {
            int synced = vehicleSyncService.syncAllConnected();
            if (synced > 0) {
                log.info("Scheduled vehicle sync updated {} vehicle(s)", synced);
            }
        } catch (Exception e) {
            log.warn("Scheduled vehicle sync error: {}", e.getMessage());
        }
    }
}
