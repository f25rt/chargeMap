# Requirements — ChargeMap PH Backend MVP

## Introduction

ChargeMap PH is a mobile-first platform that helps EV owners in Metro Cebu find the
nearest, cheapest, and currently available EV charging station. This document defines
the requirements for the **backend MVP** only (the API and data layer). The mobile
application is a separate effort.

The core promise the backend must support:

> "Find the nearest, cheapest, and available EV charger."

### Technology decisions

- **Language / framework:** Java 21, Spring Boot 3.x
- **Data store:** MongoDB (using Spring Data MongoDB), replacing the PostgreSQL/PostGIS
  design from the original product spec. Geographic queries use MongoDB `2dsphere`
  geospatial indexes and GeoJSON.
- **Security:** Spring Security with JWT authentication, password hashing (BCrypt),
  and role-based authorization (USER, OPERATOR, ADMIN).
- **Infrastructure:** Docker Compose for local MongoDB; the API packaged as a modular
  Spring Boot monolith (not microservices for the MVP).

### MVP scope (backend)

Derived from sections 27 and 41 of the product spec, the backend MVP must enable a
client to:

1. List and retrieve charging stations.
2. Find stations near a location (radius / nearest).
3. Find the cheapest stations nearby.
4. Find currently available stations nearby.
5. Search stations by text (name, operator, area, connector, charger type).
6. Filter stations by charger type, connector, speed, price, and availability.
7. Retrieve station details including chargers, pricing, and availability state.
8. Accept user reports of real-world charger status.
9. Register and authenticate users; support anonymous browsing.
10. Manage user vehicle profiles and favorites (foundation for compatibility/alerts).

Explicitly **out of scope** for this MVP: operator portal, notifications, reputation
scoring, price history charts, road-trip planner, payments, reservations, real-time
operator APIs, and the mobile client. These are noted where the data model must leave
room for them.

---

## Requirements

### Requirement 1 — Station discovery and retrieval

**User Story:** As an EV owner, I want to browse and open charging stations, so that I
can see where I can charge.

#### Acceptance Criteria

1. WHEN a client requests `GET /api/stations` THEN the system SHALL return a paginated
   list of stations with summary fields (id, name, operator, location, indicative
   price, charger counts, availability summary).
2. WHEN a client requests `GET /api/stations/{id}` for an existing station THEN the
   system SHALL return the full station detail including chargers, current pricing,
   availability state, operating hours, amenities, data source, confidence level, and
   last-verified timestamp.
3. WHEN a client requests `GET /api/stations/{id}` for a non-existent id THEN the
   system SHALL respond with HTTP 404 and a structured error body.
4. WHEN any station is returned THEN the system SHALL include a `lastUpdated`
   timestamp and SHALL NOT present availability as real-time.

### Requirement 2 — Geographic queries

**User Story:** As an EV owner on the move, I want to find chargers near me, so that I
can pick a reachable station.

#### Acceptance Criteria

1. WHEN a client requests `GET /api/stations/nearby?lat={lat}&lng={lng}&radius={r}`
   THEN the system SHALL return stations within `r` kilometers ordered by distance,
   including a computed `distanceMeters` for each station.
2. IF `lat`, `lng`, or `radius` are missing or out of valid range THEN the system
   SHALL respond with HTTP 400 and a validation error.
3. WHEN a nearby query is executed THEN the system SHALL use a MongoDB `2dsphere`
   index on the station `location` field.
4. WHEN `radius` is not provided THEN the system SHALL apply a default radius of 5 km.
5. WHEN the number of results is large THEN the system SHALL cap results to a
   configurable maximum (default 50).

### Requirement 3 — Cheapest nearby

**User Story:** As a cost-conscious EV owner, I want to see the cheapest chargers near
me, so that I can save money.

#### Acceptance Criteria

1. WHEN a client requests `GET /api/stations/cheapest?lat={lat}&lng={lng}&radius={r}`
   THEN the system SHALL return stations within the radius ordered by current
   effective price per kWh (ascending), then by distance.
2. WHEN a station has no active pricing record THEN the system SHALL exclude it from
   cheapest results.
3. WHEN two stations share the same price THEN the system SHALL order the closer
   station first.

### Requirement 4 — Available nearby

**User Story:** As an EV owner who needs to charge now, I want to see which nearby
stations have an available charger, so that I do not drive to an occupied station.

#### Acceptance Criteria

1. WHEN a client requests `GET /api/stations/available?lat={lat}&lng={lng}&radius={r}`
   THEN the system SHALL return stations within the radius that have at least one
   charger in `AVAILABLE` state, ordered by distance.
2. WHEN availability is returned THEN the system SHALL include the count of available
   chargers and the timestamp the availability was last updated.
3. WHEN a station's most recent availability information is older than a configurable
   staleness threshold THEN the system SHALL mark the station availability as
   `UNKNOWN` rather than `AVAILABLE`.

### Requirement 5 — Search

**User Story:** As an EV owner, I want to search for stations, places, and charger
types, so that I can find what I am looking for by name.

#### Acceptance Criteria

1. WHEN a client requests `GET /api/stations/search?q={query}` THEN the system SHALL
   return stations whose name, operator, address, or area matches the query.
2. WHEN the query matches a charger type or connector keyword (e.g. "DC fast",
   "CCS", "CHAdeMO") THEN the system SHALL include stations offering that type.
3. WHEN the query is empty or shorter than 2 characters THEN the system SHALL respond
   with HTTP 400.
4. WHEN a text search is performed THEN the system SHALL use a MongoDB text index.

### Requirement 6 — Filters

**User Story:** As an EV owner, I want to filter stations, so that I only see options
that fit my needs.

#### Acceptance Criteria

1. WHEN a client supplies filter parameters (`chargerType`, `connector`, `minKw`,
   `maxKw`, `priceMax`, `availableOnly`) on a station list or geo query THEN the
   system SHALL return only stations matching all supplied filters.
2. WHEN `availableOnly=true` THEN the system SHALL apply the same staleness rule as
   Requirement 4.3.
3. WHEN a filter value is invalid (unknown connector, negative kW) THEN the system
   SHALL respond with HTTP 400.

### Requirement 7 — Station sub-resources

**User Story:** As a client app, I want to fetch chargers, pricing, and availability
for a station, so that I can render detail screens.

#### Acceptance Criteria

1. WHEN a client requests `GET /api/stations/{id}/chargers` THEN the system SHALL
   return the chargers for that station.
2. WHEN a client requests `GET /api/stations/{id}/pricing` THEN the system SHALL
   return the current pricing and available price history.
3. WHEN a client requests `GET /api/stations/{id}/availability` THEN the system SHALL
   return the current availability state per charger and the last-updated timestamp.
4. WHEN the station id does not exist for any sub-resource THEN the system SHALL
   respond with HTTP 404.

### Requirement 8 — User availability reporting

**User Story:** As an EV owner at a station, I want to report the real status of a
charger, so that other users get accurate information.

#### Acceptance Criteria

1. WHEN an authenticated user submits `POST /api/stations/{id}/reports` with a valid
   status (`AVAILABLE`, `OCCUPIED`, `BROKEN`, `CLOSED`, `UNKNOWN`) THEN the system
   SHALL persist the report with the user id, station id, optional charger id, status,
   and creation timestamp.
2. WHEN a report is persisted THEN the system SHALL update the affected charger's
   status and the station's `lastUpdated`/`lastVerified` timestamps.
3. WHEN an unauthenticated request is made to submit a report THEN the system SHALL
   respond with HTTP 401.
4. WHEN the submitted status is invalid or the charger id does not belong to the
   station THEN the system SHALL respond with HTTP 400.
5. WHEN a report is accepted THEN the system SHALL respond with a confirmation body.

### Requirement 9 — Authentication and accounts

**User Story:** As a user, I want to browse anonymously but sign in to contribute, so
that I have low friction but can still report and save vehicles.

#### Acceptance Criteria

1. WHEN a client accesses read-only station endpoints (list, detail, nearby, cheapest,
   available, search, sub-resources) THEN the system SHALL allow the request without
   authentication.
2. WHEN a client submits `POST /api/users` with email, password, and name THEN the
   system SHALL create a user with a hashed password and role `USER`.
3. WHEN a client submits valid credentials to the login endpoint THEN the system SHALL
   return a signed JWT access token.
4. WHEN a request presents a valid JWT THEN the system SHALL authenticate the user and
   authorize based on role.
5. WHEN a request to a protected endpoint presents no or an invalid token THEN the
   system SHALL respond with HTTP 401.
6. WHEN a duplicate email is registered THEN the system SHALL respond with HTTP 409.
7. WHEN a client requests `GET /api/users/me` with a valid token THEN the system SHALL
   return the current user's profile.

### Requirement 10 — Vehicle profiles

**User Story:** As an EV owner, I want to store my vehicles, so that the app can later
show compatible chargers.

#### Acceptance Criteria

1. WHEN an authenticated user submits `POST /api/users/me/vehicles` with make, model,
   battery capacity, connector type, and max AC/DC power THEN the system SHALL create
   a vehicle owned by that user.
2. WHEN an authenticated user requests `GET /api/users/me/vehicles` THEN the system
   SHALL return only that user's vehicles.
3. WHEN a user updates or deletes a vehicle they own THEN the system SHALL apply the
   change; IF the vehicle belongs to another user THEN the system SHALL respond with
   HTTP 403 or 404.

### Requirement 11 — Favorites

**User Story:** As a user, I want to favorite stations, so that I can quickly return to
them.

#### Acceptance Criteria

1. WHEN an authenticated user calls `POST /api/users/me/favorites/{stationId}` THEN
   the system SHALL add the station to that user's favorites.
2. WHEN an authenticated user calls `DELETE /api/users/me/favorites/{stationId}` THEN
   the system SHALL remove it.
3. WHEN an authenticated user calls `GET /api/users/me/favorites` THEN the system
   SHALL return that user's favorite stations.
4. WHEN the referenced station does not exist THEN the system SHALL respond with 404.

### Requirement 12 — Data quality metadata

**User Story:** As the platform, I want each station to carry provenance, so that the
app can prioritize trustworthy data.

#### Acceptance Criteria

1. WHEN a station is stored THEN the system SHALL record a `dataSource`
   (`OPEN_DATASET`, `OPERATOR`, `OPERATOR_API`, `USER_SUBMISSION`) and a
   `confidence` level (`HIGH`, `MEDIUM`, `LOW`).
2. WHEN a station is returned THEN the system SHALL include `dataSource`,
   `confidence`, and `lastVerified`.

### Requirement 13 — Cross-cutting API behavior

**User Story:** As a client developer, I want consistent, secure API behavior, so that
integration is predictable.

#### Acceptance Criteria

1. WHEN any request fails validation THEN the system SHALL return a consistent error
   body with a code, message, and (where relevant) field details.
2. WHEN list endpoints are called THEN the system SHALL support pagination parameters
   (`page`, `size`) with sane defaults and a maximum page size.
3. WHEN write endpoints receive a request body THEN the system SHALL validate inputs
   (bean validation) before processing.
4. WHEN the API is running THEN the system SHALL expose a health endpoint and
   OpenAPI/Swagger documentation.
5. WHEN prices are represented THEN the system SHALL use the Philippine Peso (PHP) and
   a decimal type appropriate for currency.
