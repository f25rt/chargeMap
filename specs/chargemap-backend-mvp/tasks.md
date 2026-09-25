# Implementation Plan — ChargeMap PH Backend MVP

Incremental, test-driven build of the MongoDB-backed Spring Boot monolith described in
`design.md`. Each task is a concrete coding step; complete them in order. Requirement
references point to `requirements.md`.

- [ ] 1. Bootstrap the project and infrastructure
  - Create a Spring Boot 3.x (Java 21) project with dependencies: Spring Web, Spring
    Data MongoDB, Spring Security, Validation, Spring Data Redis, Actuator,
    springdoc-openapi, Lombok (optional), Testcontainers (test).
  - Add package-by-feature structure under `ph.chargemap` (config, common, station,
    charger, availability, pricing, report, user, vehicle, security).
  - Add `docker-compose.yml` with `mongo` and `redis` services and named volumes.
  - Add `application.yml` with Mongo URI, Redis, JWT secret/expiry (from env),
    `chargemap.geo.default-radius-km=5`, `chargemap.geo.max-results=50`,
    `chargemap.availability.staleness-minutes`, and pagination max page size.
  - Verify the app starts and connects to Mongo/Redis; expose Actuator health.
  - _Requirements: 13.4_

- [ ] 2. Common web plumbing
  - [ ] 2.1 Error model and global handler
    - Implement `ErrorResponse` and a `@RestControllerAdvice` mapping validation →
      400, unauthorized → 401, forbidden → 403, not found → 404, duplicate → 409,
      fallback → 500.
    - _Requirements: 13.1, 1.3_
  - [ ] 2.2 Pagination + geo utilities
    - Implement `PageResponse<T>`, page/size parsing with defaults and max size, and a
      geo mapper converting between GeoJSON `[lng,lat]` and DTO `{lat,lng}`, plus
      km↔meters helpers.
    - _Requirements: 13.2_

- [ ] 3. Station domain and persistence
  - [ ] 3.1 Documents and enums
    - Implement `Station` document with embedded `Charger` and `CurrentPricing`,
      GeoJSON `location`, availability summary fields, and data-quality fields. Add
      enums: `ChargerType`, `ConnectorType`, `ChargerStatus`, `DataSource`,
      `Confidence`, `PricingModel`. Use `Decimal128`/`BigDecimal` for price (PHP).
    - _Requirements: 1.2, 12.1, 13.5_
  - [ ] 3.2 Indexes
    - Configure `2dsphere` on `location`, text index on `name/operator/address/area`,
      indexes on `chargers.connectorType/chargerType/powerKw`,
      `currentPricing.pricePerKwh`, `availabilitySummary`, `availabilityUpdatedAt`.
    - Add an integration test (Testcontainers Mongo) asserting indexes exist.
    - _Requirements: 2.3, 5.4_
  - [ ] 3.3 Repository + seed data
    - Create `StationRepository` (Spring Data Mongo). Add a dev seed loader inserting
      ~50–100 sample Metro Cebu stations with varied prices/connectors/availability.
    - _Requirements: 1.1_

- [ ] 4. Station read API (list, detail, sub-resources)
  - [ ] 4.1 DTOs and mapper
    - Implement `StationSummaryDto`, `StationDetailDto`, `ChargerDto`, `PricingDto`,
      `AvailabilityDto`, and the station mapper (coordinate swap, price formatting).
    - _Requirements: 1.1, 1.2_
  - [ ] 4.2 List and detail endpoints
    - `GET /api/stations` (paginated summaries) and `GET /api/stations/{id}` (detail),
      404 on unknown id. Every response carries `lastUpdated`; availability never
      presented as real-time.
    - Web-layer tests for 200 shape and 404.
    - _Requirements: 1.1, 1.2, 1.3, 1.4_
  - [ ] 4.3 Sub-resource endpoints
    - `GET /api/stations/{id}/chargers`, `/pricing` (current + history from
      `price_history`), `/availability` (per-charger + last-updated). 404 on unknown id.
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [ ] 5. Availability computation and staleness
  - Implement `AvailabilityService.effectiveSummary(station, now)` applying the
    configurable staleness threshold (downgrade to `UNKNOWN` when stale). Wire it into
    the station mapper so all serialized stations respect the rule and expose
    `availabilityUpdatedAt`.
  - Unit tests for fresh vs stale downgrade.
  - _Requirements: 4.2, 4.3_

- [ ] 6. Geospatial query endpoints
  - [ ] 6.1 Nearby
    - Implement `$geoNear` aggregation returning `distanceMeters`, default radius 5 km,
      capped at max results; validate lat/lng/radius (400 on invalid/out-of-range).
    - Testcontainers integration test asserting distance ordering and radius bound.
    - _Requirements: 2.1, 2.2, 2.4, 2.5, 2.3_
  - [ ] 6.2 Cheapest nearby
    - `$geoNear` + `$match` (exclude null price) + `$sort` by price then distance.
    - Integration test for price-then-distance ordering and exclusion of unpriced.
    - _Requirements: 3.1, 3.2, 3.3_
  - [ ] 6.3 Available nearby
    - `$geoNear` + `$match` on `availableCount>0` and `availabilityUpdatedAt >= cutoff`
      (staleness). Include available counts and update timestamp.
    - Integration test asserting stale stations are excluded.
    - _Requirements: 4.1, 4.2, 4.3_

- [ ] 7. Filters and search
  - [ ] 7.1 Filters
    - Add `chargerType`, `connector`, `minKw`, `maxKw`, `priceMax`, `availableOnly`
      params to list and geo endpoints, injected as `$match`; validate enums/ranges
      (400 on invalid). `availableOnly` reuses the staleness rule.
    - _Requirements: 6.1, 6.2, 6.3_
  - [ ] 7.2 Search
    - `GET /api/stations/search?q=` using `$text`; reject q shorter than 2 chars (400).
      Add charger-type/connector keyword mapping so type searches also match.
    - Integration test for text match and keyword mapping; web test for 400.
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [ ] 8. User accounts and security
  - [ ] 8.1 User document + repository
    - Implement `User` document (unique email index, role, embedded vehicles,
      favoriteStationIds) and `UserRepository`.
    - _Requirements: 9.2, 12.1_
  - [ ] 8.2 JWT security infrastructure
    - Implement `BCryptPasswordEncoder`, JWT provider (sign/verify HS256, configurable
      expiry), `JwtAuthenticationFilter`, `UserDetailsService`, and `SecurityConfig`
      permitting anonymous station reads + registration/login and requiring auth
      elsewhere.
    - Security tests: anonymous read 200; protected write without token 401.
    - _Requirements: 9.1, 9.4, 9.5_
  - [ ] 8.3 Registration and login
    - `POST /api/users` (hash password, role USER, 409 on duplicate email);
      `POST /api/auth/login` (verify, return JWT); `GET /api/users/me`.
    - Tests for register/duplicate/login/me.
    - _Requirements: 9.2, 9.3, 9.6, 9.7_

- [ ] 9. User reporting
  - Implement `AvailabilityReport` document + repository and `ReportService`.
  - `POST /api/stations/{id}/reports` (auth required): validate status enum and that
    `chargerId` belongs to the station (400 otherwise); persist report; update embedded
    charger status + station `availableCount`/`availabilitySummary`/
    `availabilityUpdatedAt`/`lastUpdated`/`lastVerified`; return confirmation. 401 when
    unauthenticated.
  - Tests: successful report updates summary; invalid status/charger 400; unauth 401.
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [ ] 10. Vehicle profiles
  - `POST /api/users/me/vehicles`, `GET /api/users/me/vehicles`,
    `PUT`/`DELETE /api/users/me/vehicles/{id}` operating on embedded vehicles scoped to
    the authenticated user; ownership enforced (403/404 for others' vehicles).
  - Tests for create/list/update/delete and cross-user access rejection.
  - _Requirements: 10.1, 10.2, 10.3_

- [ ] 11. Favorites
  - `POST`/`DELETE /api/users/me/favorites/{stationId}` maintaining
    `favoriteStationIds`; `GET /api/users/me/favorites` resolving station summaries via
    `$in`. 404 when the station does not exist.
  - Tests for add/remove/list and unknown-station 404.
  - _Requirements: 11.1, 11.2, 11.3, 11.4_

- [ ] 12. Cross-cutting hardening
  - [ ] 12.1 Validation and pagination sweep
    - Ensure all request bodies use Bean Validation and all list endpoints honor
      page/size defaults and max size.
    - _Requirements: 13.1, 13.2, 13.3_
  - [ ] 12.2 Rate limiting and request logging
    - Add Redis-backed rate limiting on write + geo endpoints (report-abuse protection)
      and a request-logging filter.
    - _Requirements: 13.1 (abuse protection per product spec §37)_
  - [ ] 12.3 OpenAPI and health
    - Verify springdoc serves Swagger UI and Actuator health is exposed.
    - _Requirements: 13.4_

- [ ] 13. End-to-end verification
  - Run the full test suite (unit + Testcontainers integration + web) and fix failures.
  - Bring up Docker Compose, seed data, and manually exercise the ten MVP flows
    (list, nearby, cheapest, available, search, filter, detail, report, register/login,
    favorites) against the running API.
  - _Requirements: all_
