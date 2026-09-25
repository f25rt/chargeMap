# Vehicle Sync & Live Telemetry — Requirements

## Introduction

This spec adds an **opt-in** Vehicle Sync & Live Telemetry capability to ChargeMap PH.
Signed-in users may connect their electric vehicle manufacturer account (Tesla, Ford,
BMW, Hyundai, Kia, Mercedes-Benz, and future brands) so the platform can retrieve and
display vehicle telemetry — battery, range, location, charging status — on a dashboard.

It builds on the existing MongoDB (Spring Boot) backend and React/MUI frontend, and on
the existing USER / OPERATOR / ADMIN roles and JWT auth.

### Confirmed decisions & scope for the implemented MVP (Phase 1)

- **Asked at sign-up, optional.** Account creation asks for vehicle details and, if the
  user provides them (they may skip), connects the vehicle and runs an initial sync right
  after the account is created. Users who skip can connect later from their profile.
- **Connector abstraction.** A `VehicleConnector` interface abstracts each manufacturer.
  Real manufacturer OAuth integrations require partner API credentials that we do not
  have in this environment, so Phase 1 ships a **`MockVehicleConnector`** that produces
  realistic, evolving telemetry. This lets the whole flow (connect → sync → dashboard)
  work end to end today, and real connectors drop in later without touching the
  service/API/UI layers.
- **Token security.** OAuth tokens are stored **encrypted at rest with AES-256-GCM** via
  a dedicated crypto service. A local/dev key has a fallback; production supplies the key
  via env (`CHARGEMAP_CRYPTO_KEY`).
- **Background sync.** A scheduler refreshes connected vehicles every 5 minutes.
- **NOT in Phase 1 (moved to later phases per product decision):** notifications and
  telemetry history are **not** built in Phase 1. Also out of Phase 1: energy-cost
  tracking, fleet management, route planning, vehicle health score.

### Build phases (updated)

- **Phase 1 (implemented):** vehicle connect **at sign-up (optional)** via mock OAuth,
  vehicle + current-telemetry model, manual + scheduled 5-min sync, dashboard
  (battery/range/charging/location), battery-tier display, AES-256 token encryption,
  consent tracking, disconnect. **No notifications, no telemetry history.**
- **Phase 2 (roadmap):** **geofencing** (home/office/charging/custom zones with
  enter/exit alerts). (Real manufacturer OAuth connectors also land here.)
- **Phase 3 (roadmap):** **AI battery prediction** (remaining duration, estimated
  depletion/charge time).

---

## Requirements

### Requirement 1 — Vehicle connection asked at sign-up (optional)

**User Story:** As a new user, I want to be asked for my EV details when I create my
account, so that my vehicle is connected and synced from the start — while still being
able to skip and add it later.

#### Acceptance Criteria

1. WHEN a user creates an account THE sign-up flow SHALL present an **optional** vehicle
   step (manufacturer, model, year, nickname, location-tracking consent) with a clear
   **Skip** option.
2. WHEN the user provides vehicle details and submits THE system SHALL create the account
   AND then connect the vehicle and run an initial sync; recording the consent given.
3. WHEN the user skips THE account SHALL be created normally with no vehicle, and the user
   SHALL be able to connect one later from their profile.
4. WHERE real OAuth is available for a provider THE system SHALL exchange an authorization
   code for tokens. WHERE real OAuth is not configured THE system SHALL use the mock
   connector to simulate a successful authorization.
5. WHEN authorization succeeds THE system SHALL store the access + refresh tokens
   **encrypted** and immediately run an initial sync.
6. IF connecting a vehicle fails during sign-up THE account SHALL still be created; the
   failure SHALL be surfaced and the user left in the no-vehicle state.
7. A user SHALL be able to connect more than one vehicle and give each a nickname.

### Requirement 2 — Battery monitoring

**User Story:** As a user, I want to see my vehicle's battery level and range, so that I
know when charging is required.

#### Acceptance Criteria

1. WHEN a vehicle is connected THE dashboard SHALL show battery percentage, estimated
   remaining range, charging status, and (when available) battery health.
2. THE system SHALL classify battery level into tiers: 80–100 Excellent, 50–79 Good,
   20–49 Low, 0–19 Critical, and reflect the tier visually.
3. THE dashboard SHALL show the last-synced timestamp and an auto-refresh indicator.

### Requirement 3 — Location tracking

**User Story:** As a user, I want to see my vehicle's current location, so that I can
locate it.

#### Acceptance Criteria

1. WHEN telemetry includes coordinates THE system SHALL show the vehicle on a map with a
   marker, its battery %, charging status, and last-update time.
2. THE system SHALL expose the latest known location via `GET /api/vehicle/location`.
3. IF no location has ever been received THE map SHALL show an appropriate empty state.

### Requirement 4 — Synchronization (manual + automatic)

**User Story:** As a user, I want my vehicle data kept current automatically, so the
dashboard reflects reality without manual effort.

#### Acceptance Criteria

1. THE system SHALL provide `POST /api/vehicle/sync` to sync on demand and return the new
   `lastSync` time.
2. THE system SHALL run a background sync for all connected vehicles every 5 minutes.
3. WHEN a token is expired or near expiry THE system SHALL refresh it before calling the
   provider; IF refresh fails THE vehicle SHALL be marked as needing re-connection.
4. EACH successful sync SHALL update the vehicle's current telemetry snapshot.

> Notifications and telemetry-history persistence are **not** part of Phase 1.

### Requirement 6 — Disconnect & data control (consent / compliance)

**User Story:** As a user, I want to disconnect a vehicle and remove its data, so that I
stay in control of my information.

#### Acceptance Criteria

1. THE user SHALL be able to disconnect any connected vehicle.
2. WHEN a user disconnects a vehicle THE system SHALL delete its stored tokens + current
   telemetry and stop syncing it.
3. THE system SHALL record consent for location tracking separately, and honor its
   withdrawal by omitting location from sync + dashboard.

### Requirement 7 — Security & access control

#### Acceptance Criteria

1. All `/api/vehicle/**` endpoints SHALL require authentication and SHALL only ever
   read/write the calling user's own vehicles.
2. OAuth tokens SHALL be encrypted at rest (AES-256-GCM). Plaintext tokens SHALL never be
   returned by any API.
3. THE existing JWT auth, refresh, RBAC, and rate limiting SHALL apply unchanged.
