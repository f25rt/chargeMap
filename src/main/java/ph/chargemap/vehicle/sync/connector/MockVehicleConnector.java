package ph.chargemap.vehicle.sync.connector;

import org.springframework.stereotype.Component;
import ph.chargemap.vehicle.sync.ChargingStatus;
import ph.chargemap.vehicle.sync.Manufacturer;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 1 simulated connector. Produces realistic, evolving telemetry so the entire sync
 * pipeline (dashboard, tiers, map) works without real manufacturer API credentials.
 * Battery drains while idle/driving and rises while charging; range tracks battery;
 * location jitters around a Cebu anchor.
 *
 * <p>State is per provider-vehicle-id and held in memory — fine for a demo/dev connector.
 */
@Component
public class MockVehicleConnector implements VehicleConnector {

    private static final double[] CEBU_ANCHOR = {10.3157, 123.8854};
    private static final double FULL_RANGE_KM = 480.0; // range at 100%

    /** Evolving per-vehicle state. */
    private static final class State {
        double battery;
        ChargingStatus status;
        double lat;
        double lng;
        double odometer;
        Instant lastTick;
    }

    private final Map<String, State> states = new ConcurrentHashMap<>();
    private final Random rnd = new Random();

    @Override
    public Manufacturer manufacturer() {
        return Manufacturer.MOCK;
    }

    @Override
    public TokenSet authorize(String authCodeOrMock) {
        return new TokenSet(
                "mock-access-" + Long.toHexString(rnd.nextLong()),
                "mock-refresh-" + Long.toHexString(rnd.nextLong()),
                Instant.now().plusSeconds(3600));
    }

    @Override
    public TokenSet refresh(String refreshToken) {
        return new TokenSet(
                "mock-access-" + Long.toHexString(rnd.nextLong()),
                refreshToken,
                Instant.now().plusSeconds(3600));
    }

    @Override
    public List<VehicleInfo> listVehicles(String accessToken) {
        String id = "mock-veh-" + Integer.toHexString(rnd.nextInt(0x1000000));
        return List.of(new VehicleInfo(id, "Model Demo", 2024, "MOCKVIN" + id.toUpperCase()));
    }

    @Override
    public Telemetry fetchTelemetry(String accessToken, String providerVehicleId) {
        State s = states.computeIfAbsent(providerVehicleId, k -> {
            State init = new State();
            init.battery = 55 + rnd.nextInt(30);        // start 55–84%
            init.status = ChargingStatus.IDLE;
            init.lat = CEBU_ANCHOR[0] + (rnd.nextDouble() - 0.5) * 0.02;
            init.lng = CEBU_ANCHOR[1] + (rnd.nextDouble() - 0.5) * 0.02;
            init.odometer = 10000 + rnd.nextInt(40000);
            init.lastTick = Instant.now();
            return init;
        });

        evolve(s);

        double range = Math.round((s.battery / 100.0) * FULL_RANGE_KM * 10) / 10.0;
        double speed = s.status == ChargingStatus.CHARGING ? 45 + rnd.nextInt(105) : 0;
        int health = 90 + rnd.nextInt(8); // 90–97%
        return new Telemetry(
                (int) Math.round(s.battery), range, s.status, speed,
                s.lat, s.lng, Math.round(s.odometer * 10) / 10.0, health);
    }

    /** Advances the simulated state a little on each fetch. */
    private void evolve(State s) {
        s.lastTick = Instant.now();
        switch (s.status) {
            case CHARGING -> {
                s.battery = Math.min(100, s.battery + 3 + rnd.nextDouble() * 4);
                if (s.battery >= 100) {
                    s.status = ChargingStatus.COMPLETE;
                }
            }
            case COMPLETE -> s.status = ChargingStatus.IDLE;
            default -> { // IDLE / DISCONNECTED: light driving / parasitic drain
                s.battery = Math.max(0, s.battery - (1 + rnd.nextDouble() * 3));
                s.odometer += rnd.nextDouble() * 6;
                s.lat += (rnd.nextDouble() - 0.5) * 0.004;
                s.lng += (rnd.nextDouble() - 0.5) * 0.004;
                if (s.battery <= 15) {
                    s.status = ChargingStatus.CHARGING;
                }
            }
        }
    }
}
