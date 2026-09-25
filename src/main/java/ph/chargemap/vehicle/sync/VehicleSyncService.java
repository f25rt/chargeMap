package ph.chargemap.vehicle.sync;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ph.chargemap.common.crypto.TokenCryptoService;
import ph.chargemap.common.error.BadRequestException;
import ph.chargemap.common.error.NotFoundException;
import ph.chargemap.user.UserService;
import ph.chargemap.vehicle.sync.VehicleSyncDtos.ConnectRequest;
import ph.chargemap.vehicle.sync.VehicleSyncDtos.ConnectedVehicleDto;
import ph.chargemap.vehicle.sync.VehicleSyncDtos.LocationDto;
import ph.chargemap.vehicle.sync.VehicleSyncDtos.SyncResponse;
import ph.chargemap.vehicle.sync.VehicleSyncDtos.TelemetryDto;
import ph.chargemap.vehicle.sync.connector.Telemetry;
import ph.chargemap.vehicle.sync.connector.TokenSet;
import ph.chargemap.vehicle.sync.connector.VehicleConnector;
import ph.chargemap.vehicle.sync.connector.VehicleInfo;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Vehicle Sync Phase 1: connect (optional at sign-up), manual + scheduled sync, status,
 * location, disconnect. All operations are scoped to the calling user. OAuth tokens and
 * VIN are encrypted at rest and never leave the service in plaintext.
 */
@Service
public class VehicleSyncService {

    private static final Logger log = LoggerFactory.getLogger(VehicleSyncService.class);
    private static final long REFRESH_SKEW_SECONDS = 120;

    private final ConnectedVehicleRepository vehicleRepository;
    private final VehicleTelemetryRepository telemetryRepository;
    private final OAuthTokenRepository tokenRepository;
    private final TokenCryptoService crypto;
    private final UserService userService;
    private final Map<Manufacturer, VehicleConnector> connectors = new EnumMap<>(Manufacturer.class);

    public VehicleSyncService(ConnectedVehicleRepository vehicleRepository,
                              VehicleTelemetryRepository telemetryRepository,
                              OAuthTokenRepository tokenRepository,
                              TokenCryptoService crypto,
                              UserService userService,
                              List<VehicleConnector> connectorBeans) {
        this.vehicleRepository = vehicleRepository;
        this.telemetryRepository = telemetryRepository;
        this.tokenRepository = tokenRepository;
        this.crypto = crypto;
        this.userService = userService;
        for (VehicleConnector c : connectorBeans) {
            connectors.put(c.manufacturer(), c);
        }
    }

    private VehicleConnector connectorFor(Manufacturer m) {
        // Phase 1: real manufacturer connectors aren't configured, so fall back to MOCK so
        // the flow works end to end. Phase 2 registers real connectors per manufacturer.
        VehicleConnector c = connectors.get(m);
        if (c == null) {
            c = connectors.get(Manufacturer.MOCK);
        }
        if (c == null) {
            throw new BadRequestException("No connector available for " + m);
        }
        return c;
    }

    /**
     * Connects a vehicle for the current user and runs an initial sync. Wrapped in a
     * transaction so the vehicle + token + telemetry writes are atomic.
     */
    @Transactional
    public ConnectedVehicleDto connect(ConnectRequest req) {
        if (req == null || req.manufacturer() == null) {
            throw new BadRequestException("Manufacturer is required");
        }
        ObjectId userId = userService.requireActiveUser().getId();
        VehicleConnector connector = connectorFor(req.manufacturer());

        TokenSet tokens = connector.authorize(req.authCode());

        List<VehicleInfo> found = connector.listVehicles(tokens.accessToken());
        if (found.isEmpty()) {
            throw new BadRequestException("No vehicles found on the connected account");
        }
        VehicleInfo info = found.get(0);

        Instant now = Instant.now();
        ConnectedVehicle v = new ConnectedVehicle();
        v.setUserId(userId);
        v.setManufacturer(req.manufacturer());
        v.setModel(info.model());
        v.setYear(info.year());
        v.setVinEnc(crypto.encrypt(info.vin()));
        v.setNickname(req.nickname() != null && !req.nickname().isBlank()
                ? req.nickname().trim() : info.model());
        v.setProviderVehicleId(info.providerVehicleId());
        v.setLocationConsent(req.locationConsent() == null || req.locationConsent());
        if (req.chargeTargetPercent() != null) {
            v.setChargeTargetPercent(Math.max(1, Math.min(100, req.chargeTargetPercent())));
        }
        v.setConnected(true);
        v.setCreatedAt(now);
        v.setUpdatedAt(now);
        v.setLastSyncAt(now);
        ConnectedVehicle saved = vehicleRepository.save(v);

        OAuthToken token = new OAuthToken();
        token.setUserId(userId);
        token.setVehicleId(saved.getId());
        token.setProvider(req.manufacturer());
        token.setAccessTokenEnc(crypto.encrypt(tokens.accessToken()));
        token.setRefreshTokenEnc(crypto.encrypt(tokens.refreshToken()));
        token.setExpiresAt(tokens.expiresAt());
        tokenRepository.save(token);

        syncVehicle(saved, connector, tokens.accessToken());

        return toDto(saved);
    }

    /** Manual sync of all the current user's connected vehicles. */
    public SyncResponse syncCurrentUser() {
        ObjectId userId = userService.requireCurrentUser().getId();
        List<ConnectedVehicle> vehicles = vehicleRepository.findByUserIdOrderByCreatedAtAsc(userId);
        int synced = 0;
        Instant now = Instant.now();
        for (ConnectedVehicle v : vehicles) {
            if (v.isConnected() && trySync(v)) {
                synced++;
            }
        }
        return new SyncResponse(true, now, synced);
    }

    /** Scheduled batch sync across all users (called by the scheduler). */
    public int syncAllConnected() {
        List<ConnectedVehicle> vehicles = vehicleRepository.findByConnectedTrue();
        int synced = 0;
        for (ConnectedVehicle v : vehicles) {
            if (trySync(v)) {
                synced++;
            }
        }
        return synced;
    }

    private boolean trySync(ConnectedVehicle v) {
        try {
            OAuthToken token = tokenRepository.findByVehicleId(v.getId()).orElse(null);
            if (token == null) {
                return false;
            }
            VehicleConnector connector = connectorFor(v.getManufacturer());
            String accessToken = validAccessToken(v, token, connector);
            syncVehicle(v, connector, accessToken);
            return true;
        } catch (Exception e) {
            log.warn("Sync failed for vehicle {}: {}", v.getId(), e.getMessage());
            return false;
        }
    }

    /** Refreshes the access token if expired or near expiry; persists the new token. */
    private String validAccessToken(ConnectedVehicle v, OAuthToken token, VehicleConnector connector) {
        Instant expiry = token.getExpiresAt();
        boolean nearExpiry = expiry == null
                || expiry.isBefore(Instant.now().plusSeconds(REFRESH_SKEW_SECONDS));
        if (!nearExpiry) {
            return crypto.decrypt(token.getAccessTokenEnc());
        }
        try {
            TokenSet refreshed = connector.refresh(crypto.decrypt(token.getRefreshTokenEnc()));
            token.setAccessTokenEnc(crypto.encrypt(refreshed.accessToken()));
            token.setRefreshTokenEnc(crypto.encrypt(refreshed.refreshToken()));
            token.setExpiresAt(refreshed.expiresAt());
            tokenRepository.save(token);
            return refreshed.accessToken();
        } catch (Exception e) {
            v.setConnected(false);
            v.setUpdatedAt(Instant.now());
            vehicleRepository.save(v);
            throw new BadRequestException("Token refresh failed; vehicle needs re-connection");
        }
    }

    /** Fetches telemetry and updates the current snapshot (respecting location consent). */
    private void syncVehicle(ConnectedVehicle v, VehicleConnector connector, String accessToken) {
        Telemetry t = connector.fetchTelemetry(accessToken, v.getProviderVehicleId());
        Instant now = Instant.now();

        VehicleTelemetry snap = telemetryRepository.findByVehicleId(v.getId())
                .orElseGet(() -> {
                    VehicleTelemetry s = new VehicleTelemetry();
                    s.setVehicleId(v.getId());
                    return s;
                });
        snap.setBatteryPercentage(t.batteryPercentage());
        snap.setRangeKm(t.rangeKm());
        snap.setChargingStatus(t.chargingStatus());
        snap.setChargingSpeedKw(t.chargingSpeedKw());
        snap.setOdometerKm(t.odometerKm());
        snap.setBatteryHealthPercent(t.batteryHealthPercent());
        if (v.isLocationConsent()) {
            snap.setLatitude(t.latitude());
            snap.setLongitude(t.longitude());
        } else {
            snap.setLatitude(null);
            snap.setLongitude(null);
        }
        snap.setLastUpdated(now);
        telemetryRepository.save(snap);

        v.setLastSyncAt(now);
        v.setUpdatedAt(now);
        vehicleRepository.save(v);
    }

    // ----- Reads -----

    public List<ConnectedVehicleDto> listCurrentUser() {
        ObjectId userId = userService.requireCurrentUser().getId();
        return vehicleRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    /** Status for a specific vehicle, or the user's primary (first) one. */
    public ConnectedVehicleDto status(String vehicleId) {
        ConnectedVehicle v = (vehicleId != null) ? loadOwned(vehicleId) : primaryOrThrow();
        return toDto(v);
    }

    public LocationDto location(String vehicleId) {
        ConnectedVehicle v = (vehicleId != null) ? loadOwned(vehicleId) : primaryOrThrow();
        VehicleTelemetry snap = telemetryRepository.findByVehicleId(v.getId()).orElse(null);
        if (snap == null || !v.isLocationConsent()) {
            return new LocationDto(null, null, snap == null ? null : snap.getLastUpdated());
        }
        return new LocationDto(snap.getLatitude(), snap.getLongitude(), snap.getLastUpdated());
    }

    @Transactional
    public void disconnect(String vehicleId) {
        ConnectedVehicle v = loadOwned(vehicleId);
        tokenRepository.deleteByVehicleId(v.getId());
        telemetryRepository.deleteByVehicleId(v.getId());
        vehicleRepository.delete(v);
    }

    // ----- helpers -----

    private ConnectedVehicle primaryOrThrow() {
        ObjectId userId = userService.requireCurrentUser().getId();
        return vehicleRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No connected vehicle"));
    }

    private ConnectedVehicle loadOwned(String vehicleId) {
        if (vehicleId == null || !ObjectId.isValid(vehicleId)) {
            throw new BadRequestException("Invalid vehicle id");
        }
        ObjectId userId = userService.requireCurrentUser().getId();
        return vehicleRepository.findByIdAndUserId(new ObjectId(vehicleId), userId)
                .orElseThrow(() -> new NotFoundException("Vehicle not found"));
    }

    private ConnectedVehicleDto toDto(ConnectedVehicle v) {
        VehicleTelemetry snap = telemetryRepository.findByVehicleId(v.getId()).orElse(null);
        TelemetryDto telemetry = (snap == null) ? null : new TelemetryDto(
                snap.getBatteryPercentage(),
                VehicleSyncDtos.tierOf(snap.getBatteryPercentage()),
                snap.getRangeKm(),
                snap.getChargingStatus(),
                snap.getChargingSpeedKw(),
                snap.getBatteryHealthPercent(),
                snap.getOdometerKm(),
                snap.getLastUpdated());
        return new ConnectedVehicleDto(
                v.getId().toHexString(),
                v.getManufacturer(),
                v.getModel(),
                v.getYear(),
                v.getNickname(),
                v.isConnected(),
                v.isLocationConsent(),
                v.getChargeTargetPercent(),
                v.getLastSyncAt(),
                telemetry);
    }
}
