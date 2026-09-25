package ph.chargemap.vehicle.sync.connector;

import ph.chargemap.vehicle.sync.Manufacturer;

import java.util.List;

/**
 * Abstraction over a manufacturer's account integration. Phase 1 ships a mock
 * implementation; real OAuth-backed connectors implement the same interface later without
 * changes to the service, API, or UI layers.
 */
public interface VehicleConnector {

    Manufacturer manufacturer();

    /** Exchange an authorization code (or a mock marker) for tokens. */
    TokenSet authorize(String authCodeOrMock);

    /** Exchange a refresh token for a fresh token set. */
    TokenSet refresh(String refreshToken);

    /** List the vehicles available on the connected account. */
    List<VehicleInfo> listVehicles(String accessToken);

    /** Fetch the latest telemetry for one vehicle. */
    Telemetry fetchTelemetry(String accessToken, String providerVehicleId);
}
