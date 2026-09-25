# Vehicle Sync & Live Telemetry — Design

## Overview

An opt-in module that lets a signed-in user connect EV manufacturer accounts and view
live telemetry. It slots into the existing Spring Boot + MongoDB backend and React/MUI
frontend. Phase 1 is fully working end to end using a mock connector; real manufacturer
OAuth connectors implement the same interface later with no changes above the connector
layer.

```
Frontend (React/MUI)
    |  /api/vehicle/**  (JWT, same-origin)
    v
VehicleController  ──►  VehicleService  ──►  VehicleConnector (interface)
                                   │             ├── MockVehicleConnector (Phase 1)
                                   │             └── Tesla/Ford/BMW/... (Phase 2, real OAuth)
                                   ├──► TokenCryptoService (AES-256-GCM)
                                   └──► repositories (MongoDB)
                                              Vehicle, VehicleTelemetry, OAuthToken
VehicleSyncScheduler (@Scheduled every 5 min) ──► VehicleService.syncAll()
```

## Package layout

New package `ph.chargemap.vehicle.sync` (a **subpackage** — the existing
`ph.chargemap.vehicle` package already holds the user's saved EV *profile* feature:
`Vehicle`/`VehicleService`/`VehicleController` at `/api/users/me/vehicles`, make/model/
connector used for charger matching. The two features are unrelated and must not collide,
hence the subpackage + `Connected*` naming):

- `ConnectedVehicle`, `VehicleTelemetry`, `OAuthToken` — documents
- `ConnectedVehicleRepository`, `VehicleTelemetryRepository`, `OAuthTokenRepository`
- `VehicleSyncService` — connect / sync / disconnect / status / location
- `VehicleSyncController` — REST endpoints under `/api/vehicle`
- `VehicleSyncScheduler` — 5-minute background sync
- `connector/VehicleConnector` (interface) + `connector/MockVehicleConnector`
- `connector/Telemetry`, `connector/VehicleInfo`, `connector/TokenSet` (connector DTOs)
- DTOs in `VehicleSyncDtos`: `ConnectedVehicleDto`, `TelemetryDto`, `LocationDto`,
  `ConnectRequest`, `SyncResponse`

> Phase 1 has **no** `TelemetryHistory` and **no** notifications (moved to later phases).
> The connect flow is triggered **optionally at sign-up** rather than from an opt-in panel.

Crypto lives in `ph.chargemap.common.crypto.TokenCryptoService`.

## Data model (MongoDB collections)

### `vehicles`
| field | type | notes |
|-------|------|-------|
| id | ObjectId | |
| userId | ObjectId (indexed) | owner |
| manufacturer | enum (TESLA, FORD, BMW, HYUNDAI, KIA, MERCEDES, MOCK) | |
| model | String | |
| year | Integer | |
| vin | String | stored encrypted (sensitive) |
| nickname | String | user label |
| providerVehicleId | String | id at the manufacturer |
| locationConsent | boolean | Req 6.4 |
| chargeTargetPercent | int (default 80) | for "charging complete" |
| connected | boolean | false after disconnect / failed refresh |
| lastSyncAt | Instant | |
| createdAt / updatedAt | Instant | |

### `vehicle_telemetry` (current snapshot, one per vehicle)
vehicleId (unique), batteryPercentage, rangeKm, chargingStatus (enum
IDLE/CHARGING/COMPLETE/DISCONNECTED), chargingSpeedKw, latitude, longitude, odometerKm,
batteryHealthPercent, lastUpdated.

### `oauth_tokens`
userId, vehicleId, provider, **accessTokenEnc**, **refreshTokenEnc** (AES-256-GCM
ciphertext, never returned by any API), expiresAt. One per connected vehicle.

_(Phase 1 has no `telemetry_history` and no `vehicle_notifications` collections.)_

## Connector abstraction

```java
interface VehicleConnector {
    Manufacturer manufacturer();
    // Exchange an auth code (or mock) for tokens.
    TokenSet authorize(String authCodeOrMock);
    TokenSet refresh(String refreshToken);
    List<VehicleInfo> listVehicles(String accessToken);
    Telemetry fetchTelemetry(String accessToken, String providerVehicleId);
}
```

`MockVehicleConnector` (Phase 1) returns a deterministic-but-evolving telemetry stream:
battery drains while driving/idle and rises while charging, range tracks battery,
location jitters around a Cebu anchor. This exercises every downstream path (tiers,
notifications, history, map) without external calls. Real connectors implement the same
interface using each manufacturer's OAuth + telemetry REST APIs.

`VehicleService` selects a connector by `Manufacturer` from a registry map, so adding a
real connector is a one-line registration.

## Token encryption

`TokenCryptoService` uses **AES-256-GCM** (256-bit key, 96-bit random IV per value,
authentication tag). Ciphertext is stored Base64 as `iv:ciphertext`. Key resolution:
`CHARGEMAP_CRYPTO_KEY` (Base64 32 bytes) in prod; a derived dev key fallback otherwise
(logged as dev-only). VIN is encrypted the same way. Decryption happens only inside the
service when calling a connector — tokens/VIN are never placed in any DTO.

## Sync logic

- **Initial sync** (on connect / at sign-up): store tokens → `listVehicles` → persist
  `Vehicle`(s) → `fetchTelemetry` → save current `VehicleTelemetry`.
- **Manual sync** (`POST /api/vehicle/sync`): sync all of the caller's connected vehicles.
- **Scheduled sync** (`VehicleSyncScheduler`, `@Scheduled(fixedDelayString = 5min)`):
  loads all connected vehicles across users, validates/refreshes tokens, fetches
  telemetry, updates the current snapshot. Guarded so a failure on one vehicle doesn't
  abort the batch.
- **Token refresh:** if `expiresAt` is within a 2-minute skew, call `refresh`; on failure
  mark `connected=false`.

## API endpoints (all require auth; user-scoped)

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/vehicle` | List the caller's connected vehicles (+ current telemetry summary) |
| POST | `/api/vehicle/connect` | Begin/complete connect for a manufacturer (mock OAuth in Phase 1); body `{manufacturer, nickname?, locationConsent}` |
| POST | `/api/vehicle/sync` | Sync all caller vehicles; returns `{success, lastSync}` |
| GET | `/api/vehicle/status` | Battery/range/charging for the caller's primary (or `?vehicleId=`) vehicle |
| GET | `/api/vehicle/location` | Latest location `{latitude, longitude, lastUpdate}` |
| DELETE | `/api/vehicle/{id}` | Disconnect (delete tokens + telemetry, stop sync) |

Security: `SecurityConfig` already funnels non-public routes through `authenticated()`;
`/api/vehicle/**` is authenticated by default. Every service method filters by the current
user id from the security context.

## Frontend

- **Sign-up dialog**: an optional vehicle step (manufacturer, model, year, nickname,
  location-consent) with a **Skip** button. On submit, the collected details are sent with
  registration; the account is created, then the vehicle is connected + synced.
- **`VehiclePanel`** in the Profile sheet: if no vehicle, a "Connect vehicle" action +
  manufacturer picker; else the dashboard.
- **`VehicleDashboard`**: battery ring with tier color, range, charging chip, last-synced,
  a small map with the vehicle marker, "Sync now" (`POST /api/vehicle/sync`), disconnect.
- Polls `GET /api/vehicle/status` + `/location` every ~30s while open (server is the
  source of truth via the 5-min scheduler).
- `api` client methods + TS types mirror the DTOs. Tokens/VIN never reach the client.

## Testing / verification

- Runtime smoke: connect (mock) → status/location populated → manual sync advances
  telemetry battery/charging.
- Transactions: connect's multi-write (vehicle + token + telemetry) wrapped in
  `@Transactional` for atomicity (Atlas replica set).

## Roadmap (not built in Phase 1)

Phase 2: **geofencing** (home/office/charging/custom zones + enter/exit + unauthorized-
movement alerts); real manufacturer OAuth connectors.
Phase 3: **AI battery prediction** (remaining battery duration, estimated depletion time,
estimated charging duration).
