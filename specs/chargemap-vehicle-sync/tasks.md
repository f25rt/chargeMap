# Vehicle Sync & Live Telemetry — Tasks

## Phase 1 (this implementation) — connect at sign-up (optional), no notifications/history

- [ ] 1. Crypto: `TokenCryptoService` (AES-256-GCM, env key + dev fallback). _(Req 7.2)_
- [ ] 2. Domain models + repositories: `Vehicle`, `VehicleTelemetry`, `OAuthToken` + Mongo
      repositories. _(Req 1,2,3,4)_
- [ ] 3. Connector layer: `VehicleConnector` interface, `Telemetry`/`VehicleInfo`/`TokenSet`
      DTOs, `MockVehicleConnector` with evolving telemetry, connector registry. _(Req 1,4)_
- [ ] 4. `VehicleService`: connect (encrypt+store tokens, initial sync), syncOne/syncAll,
      token refresh, disconnect, status/location; `@Transactional` on connect. _(Req 1–7)_
- [ ] 5. `VehicleController`: `/api/vehicle/**` endpoints, user-scoped. _(Req 1–7)_
- [ ] 6. `VehicleSyncScheduler`: `@Scheduled` 5-minute `syncAll`; enable scheduling. _(Req 4.2)_
- [ ] 7. Registration: extend sign-up to accept optional vehicle details and connect+sync
      after account creation. _(Req 1)_
- [ ] 8. Frontend: add optional vehicle step to the sign-up dialog (skippable); vehicle
      dashboard in profile (battery tier, range, charging, map, sync now, disconnect);
      types + api client. _(Req 1,2,3)_
- [ ] 9. Verify: backend compile, frontend build, end-to-end mock connect→sync→status→
      location smoke test. _(all)_

## Phase 2 (roadmap — not implemented)

- [ ] Geofencing: home / office / charging-station / custom zones; enter/exit alerts;
      unauthorized-movement security alert.
- [ ] Real OAuth connectors (Tesla, Ford, BMW, Hyundai, Kia, Mercedes).

## Phase 3 (roadmap — not implemented)

- [ ] AI battery prediction: remaining battery duration, estimated depletion time,
      estimated charging duration.
