# Design — ChargeMap PH Backend MVP

## Overview

This document describes the technical design of the ChargeMap PH backend MVP. It
realizes the requirements in `requirements.md` as a modular Spring Boot monolith backed
by **MongoDB**. The original product spec proposed PostgreSQL + PostGIS; this design
replaces that relational + PostGIS layer with MongoDB documents and MongoDB's native
`2dsphere` geospatial indexing and aggregation framework. Everything else in the
product spec (module boundaries, API surface, security model, phased scope) is
preserved.

### Why MongoDB works here

- **Geospatial** — MongoDB provides `2dsphere` indexes with `$near`, `$geoNear`, and
  `$geoWithin` operators over GeoJSON. This directly replaces PostGIS `ST_DWithin` /
  `ST_Distance` for the nearby, cheapest-nearby, and available-nearby queries.
- **Document shape fits the read model** — A station detail screen needs the station,
  its chargers, and current pricing together. Embedding chargers and current pricing in
  the station document makes the dominant read (`GET /api/stations/{id}` and the geo
  queries) a single-document fetch with no joins.
- **Flexible provenance** — Data-quality metadata (source, confidence) and
  forward-looking fields (amenities, ratings) vary by station and are natural as
  optional document fields.

### High-level architecture

```
                 Mobile App (separate effort)
                          |
                          v
        +----------------------------------------+
        |         Spring Boot Monolith           |
        |                                        |
        |  Web layer (REST controllers, DTOs)    |
        |  Security (JWT filter, method security) |
        |                                        |
        |  Modules:                              |
        |   station | charger | availability     |
        |   pricing | user | vehicle | report    |
        |                                        |
        |  Data layer (Spring Data MongoDB)      |
        +----------------------------------------+
             |                        |
             v                        v
        +---------+              +---------+
        | MongoDB |              |  Redis  |
        | 2dsphere|              | cache / |
        | indexes |              | limiter |
        +---------+              +---------+
```

Chargers and current pricing are **embedded** in the station document (read-optimized).
Availability reports and price history are stored in **separate collections** because
they are append-heavy time series and are read on demand, not on every station read.

## Module structure

A single Gradle/Maven project, package-by-feature under `ph.chargemap`:

```
ph.chargemap
├── config          // Mongo, security, cache, OpenAPI, index bootstrap
├── common          // error model, pagination, geo utilities, base types
├── station         // Station document, repository, service, controller, DTOs
├── charger         // embedded Charger type + charger-type/connector enums
├── availability    // availability computation + staleness rules
├── pricing         // embedded current price + PriceHistory collection
├── report          // AvailabilityReport collection, report service
├── user            // User document, auth, registration, /users/me
├── vehicle         // embedded vehicle profiles under user
└── security        // JWT provider, filter, UserDetails
```

The module boundaries mirror the product spec's `chargemap-api` layout. Services can be
extracted later; for the MVP they are Spring `@Service` beans within one deployable.

## Data model (MongoDB collections)

Three top-level collections: `stations`, `users`, and `availability_reports`, plus
`price_history`. Ids are Mongo `ObjectId` exposed to clients as hex strings.

### `stations` collection

```jsonc
{
  "_id": "ObjectId",
  "name": "SM City Cebu",
  "operator": "Some Operator Inc.",
  "address": "North Reclamation Area, Cebu City",
  "area": "North Reclamation Area",          // for search/filter by neighborhood
  "location": {                                // GeoJSON Point — [lng, lat] order
    "type": "Point",
    "coordinates": [123.9165, 10.3116]
  },
  "openingHours": "24 hours",                  // free-text for MVP
  "phone": "+63 32 000 0000",
  "amenities": ["restroom", "cafe", "mall"],
  "rating": 4.4,

  "chargers": [                                // EMBEDDED
    {
      "chargerId": "ObjectId",                 // stable id for reports/updates
      "connectorType": "CCS2",                 // enum
      "chargerType": "DC_FAST",                // enum: AC | DC | DC_FAST
      "powerKw": 60,
      "status": "AVAILABLE",                    // AVAILABLE|OCCUPIED|BROKEN|CLOSED|UNKNOWN
      "statusUpdatedAt": "2026-09-23T10:00:00Z"
    }
  ],

  "currentPricing": {                          // EMBEDDED snapshot of active price
    "pricePerKwh": "15.00",                    // stored as Decimal128 (PHP)
    "pricingModel": "PER_KWH",
    "effectiveFrom": "2026-09-15T00:00:00Z",
    "effectiveTo": null
  },

  // Denormalized availability summary, recomputed on report/update:
  "availableCount": 2,
  "totalChargers": 4,
  "availabilitySummary": "AVAILABLE",          // AVAILABLE|OCCUPIED|UNKNOWN
  "availabilityUpdatedAt": "2026-09-23T10:00:00Z",

  // Data-quality metadata (Requirement 12):
  "dataSource": "OPERATOR",                    // OPEN_DATASET|OPERATOR|OPERATOR_API|USER_SUBMISSION
  "confidence": "HIGH",                        // HIGH|MEDIUM|LOW
  "lastVerified": "2026-09-23T10:00:00Z",

  "createdAt": "...",
  "lastUpdated": "..."
}
```

Rationale for embedding: the product spec's `Station`, `Charger`, and current
`Pricing` are always read together on the detail and map screens. Embedding removes
joins and lets geo queries return everything the map/list needs in one document. The
`chargerId` on each embedded charger preserves the identity that a relational
`charger.id` would have, so reports can target a specific charger.

Denormalized `availableCount` / `availabilitySummary` / `availabilityUpdatedAt` exist
so geo + availability queries (Requirement 4) filter and sort without post-processing
every charger array in the application.

### `availability_reports` collection (append-only)

```jsonc
{
  "_id": "ObjectId",
  "stationId": "ObjectId",
  "chargerId": "ObjectId | null",
  "userId": "ObjectId",
  "status": "OCCUPIED",                        // AVAILABLE|OCCUPIED|BROKEN|CLOSED|UNKNOWN
  "createdAt": "2026-09-23T10:07:00Z"
}
```

Kept separate from the station because reports are high-volume, append-only, and read
as history (Requirement 7.3, future reputation). Writing a report updates the embedded
charger status + station denormalized summary in the same service call.

### `price_history` collection

```jsonc
{
  "_id": "ObjectId",
  "stationId": "ObjectId",
  "pricePerKwh": "13.00",                      // Decimal128
  "pricingModel": "PER_KWH",
  "effectiveFrom": "2026-08-01T00:00:00Z",
  "effectiveTo": "2026-09-15T00:00:00Z",
  "createdAt": "..."
}
```

The active price is embedded on the station (`currentPricing`) for fast reads; superseded
prices are appended here. Supports Requirement 7.2 and the future 30-day price chart.

### `users` collection

```jsonc
{
  "_id": "ObjectId",
  "email": "user@example.com",                 // unique index
  "name": "Juan Dela Cruz",
  "passwordHash": "$2a$...",                    // BCrypt; never returned
  "role": "USER",                               // USER|OPERATOR|ADMIN
  "vehicles": [                                 // EMBEDDED vehicle profiles
    {
      "vehicleId": "ObjectId",
      "make": "BYD",
      "model": "Atto 3",
      "batteryCapacityKwh": 60.5,
      "connectorType": "CCS2",
      "maxAcKw": 7,
      "maxDcKw": 80,
      "createdAt": "...",
      "updatedAt": "..."
    }
  ],
  "favoriteStationIds": ["ObjectId"],           // references stations._id
  "createdAt": "...",
  "updatedAt": "..."
}
```

Vehicles are embedded because they are always accessed in the context of their owner
and are few per user (Requirement 10). Favorites are stored as an id array on the user
(Requirement 11); station details are resolved with an `$in` lookup when listing.

## Indexes

Created at startup via a `MongoIndexConfig` (or `@Document` + `@GeoSpatialIndexed` /
`@Indexed` annotations, applied through `MongoMappingContext`):

| Collection | Index | Purpose | Requirement |
|---|---|---|---|
| stations | `location` `2dsphere` | nearby / cheapest / available geo queries | 2.3 |
| stations | text index on `name, operator, address, area` | search | 5.4 |
| stations | `chargers.connectorType`, `chargers.chargerType`, `chargers.powerKw` | filters | 6.1 |
| stations | `currentPricing.pricePerKwh` | cheapest ordering | 3.1 |
| stations | `availabilitySummary`, `availabilityUpdatedAt` | available filter + staleness | 4.1, 4.3 |
| users | `email` unique | registration/login | 9.2, 9.6 |
| availability_reports | `stationId`, `createdAt` (compound) | report history | 7.3 |
| price_history | `stationId`, `effectiveFrom` | price history | 7.2 |

## Geospatial query design (replacing PostGIS)

All location queries use the `2dsphere` index on `stations.location`. Coordinates are
GeoJSON `[longitude, latitude]`. Distances are computed in meters and converted to km
for the API.

### Nearby (Requirement 2)

Use an aggregation with `$geoNear` so distance is returned per document:

```jsonc
[
  { "$geoNear": {
      "near": { "type": "Point", "coordinates": [lng, lat] },
      "distanceField": "distanceMeters",
      "maxDistance": radiusMeters,             // radius km * 1000
      "spherical": true,
      "query": { /* optional filters injected here */ }
  }},
  { "$limit": maxResults }                      // default 50 (Req 2.5)
]
```

`$geoNear` returns results already sorted by ascending distance, satisfying "ordered by
distance" without an extra sort. This is the MongoDB equivalent of PostGIS
`ORDER BY ST_Distance(...)`.

### Cheapest nearby (Requirement 3)

`$geoNear` to bound by radius and compute distance, then sort by price then distance,
excluding stations with no active price:

```jsonc
[
  { "$geoNear": { "near": {...}, "distanceField": "distanceMeters",
                  "maxDistance": radiusMeters, "spherical": true } },
  { "$match": { "currentPricing.pricePerKwh": { "$ne": null } } },
  { "$sort": { "currentPricing.pricePerKwh": 1, "distanceMeters": 1 } },
  { "$limit": maxResults }
]
```

### Available nearby (Requirement 4)

`$geoNear` bounded by radius, then match on the denormalized availability summary with
the staleness rule applied via a computed cutoff:

```jsonc
[
  { "$geoNear": { "near": {...}, "distanceField": "distanceMeters",
                  "maxDistance": radiusMeters, "spherical": true } },
  { "$match": {
      "availableCount": { "$gt": 0 },
      "availabilityUpdatedAt": { "$gte": stalenessCutoff }   // now - threshold
  }},
  { "$limit": maxResults }
]
```

Records older than the staleness threshold fail the `availabilityUpdatedAt` match and
are treated as `UNKNOWN` (Requirement 4.3), so they are not returned as available. The
same cutoff logic drives the read-side mapping that downgrades `availabilitySummary` to
`UNKNOWN` when serializing any station.

### Filters (Requirement 6)

Filters are injected as a `$match` (either inside the `$geoNear` `query` for geo
endpoints or as a standalone `$match`/query for the list endpoint):

- `chargerType` → `{ "chargers.chargerType": value }`
- `connector` → `{ "chargers.connectorType": value }`
- `minKw`/`maxKw` → `{ "chargers.powerKw": { "$gte": minKw, "$lte": maxKw } }`
- `priceMax` → `{ "currentPricing.pricePerKwh": { "$lte": priceMax } }`
- `availableOnly=true` → adds the availability + staleness match above

Enum/range validation happens in the web layer; invalid values return 400 (Req 6.3).

### Search (Requirement 5)

Primary path is a MongoDB `$text` search over the station text index:

```jsonc
{ "$text": { "$search": query } }
```

Charger-type / connector keyword synonyms (e.g. "DC fast" → `DC_FAST`, "CCS" →
`CCS2`/`CCS1`) are mapped in a small keyword table in the search service; when the
normalized query hits a known keyword, an `$or` branch on `chargers.chargerType` /
`chargers.connectorType` is added so type searches also match. Queries shorter than 2
characters are rejected with 400 (Req 5.3).

## Availability computation and staleness

A single `AvailabilityService` owns the rule:

- `effectiveSummary(station, now)` returns `UNKNOWN` when
  `now - station.availabilityUpdatedAt > stalenessThreshold`, otherwise the stored
  `availabilitySummary`.
- The threshold is configurable (`chargemap.availability.staleness-minutes`, default
  e.g. 30) per Requirements 4.3 and 6.2.
- On serialization, station DTOs run through this so the API never presents stale data
  as available and always exposes `availabilityUpdatedAt` ("last updated N minutes
  ago") — Requirement 1.4 / 4.2.

When a report is accepted, `ReportService`:
1. Appends the report to `availability_reports`.
2. Updates the target embedded charger's `status` + `statusUpdatedAt`.
3. Recomputes `availableCount`, `availabilitySummary`, `availabilityUpdatedAt`,
   `lastUpdated`, and `lastVerified` on the station.
4. Persists via a targeted `$set` update (positional operator on the matched charger).

## API design

REST controllers under `/api`, matching the product spec's recommended API (section 35).

### Public (no auth — Requirement 9.1)

| Method | Path | Requirement |
|---|---|---|
| GET | `/api/stations` | 1.1, 6, 13.2 |
| GET | `/api/stations/{id}` | 1.2, 1.3 |
| GET | `/api/stations/nearby` | 2 |
| GET | `/api/stations/cheapest` | 3 |
| GET | `/api/stations/available` | 4 |
| GET | `/api/stations/search` | 5 |
| GET | `/api/stations/{id}/chargers` | 7.1 |
| GET | `/api/stations/{id}/pricing` | 7.2 |
| GET | `/api/stations/{id}/availability` | 7.3 |
| POST | `/api/users` | 9.2 |
| POST | `/api/auth/login` | 9.3 |

### Authenticated (JWT — role USER+)

| Method | Path | Requirement |
|---|---|---|
| POST | `/api/stations/{id}/reports` | 8 |
| GET | `/api/users/me` | 9.7 |
| GET/POST | `/api/users/me/vehicles` | 10.1, 10.2 |
| PUT/DELETE | `/api/users/me/vehicles/{id}` | 10.3 |
| POST/DELETE | `/api/users/me/favorites/{stationId}` | 11.1, 11.2 |
| GET | `/api/users/me/favorites` | 11.3 |

### DTOs

- `StationSummaryDto` (list/geo): id, name, operator, location, distanceMeters (geo
  only), indicative price, availableCount/totalChargers, availabilitySummary,
  availabilityUpdatedAt, dataSource, confidence.
- `StationDetailDto`: summary fields + chargers, currentPricing, openingHours,
  amenities, rating, phone, lastVerified, lastUpdated.
- `ChargerDto`, `PricingDto`, `AvailabilityDto`, `ReportRequest`/`ReportResponse`,
  `RegisterUserRequest`, `LoginRequest`/`TokenResponse`, `VehicleRequest`/`VehicleDto`,
  `UserProfileDto`, `PageResponse<T>`, `ErrorResponse`.

Coordinates in DTOs are exposed as `{ lat, lng }` for client friendliness even though
they are stored GeoJSON `[lng, lat]`; a mapper handles the swap in one place.

## Security design

- **Authentication:** Stateless JWT. `POST /api/auth/login` verifies email + BCrypt
  password and returns a signed JWT (HS256, secret from config/env; expiry
  configurable). A `JwtAuthenticationFilter` validates the bearer token and populates
  the `SecurityContext`.
- **Authorization:** Method/URL security. Public station reads and registration/login
  are permitted anonymously (Req 9.1). Reports, `/users/me/**` require authentication.
  `OPERATOR`/`ADMIN` roles are defined for later phases (operator portal, admin).
- **Passwords:** `BCryptPasswordEncoder`. `passwordHash` is never serialized.
- **Rate limiting:** Redis-backed limiter (bucket per IP + per user) on write and geo
  endpoints to protect against report abuse (product spec section 37).
- **Validation:** Bean Validation on all request bodies and query params; a
  `@RestControllerAdvice` maps violations to the standard error body (Req 13.1, 13.3).
- **CORS / logging:** CORS configured for the mobile origins; request logging filter for
  auditability.

## Error handling

A single `@RestControllerAdvice` produces a consistent `ErrorResponse`:

```jsonc
{ "code": "VALIDATION_ERROR", "message": "radius must be <= 50", "fields": { "radius": "..." }, "timestamp": "..." }
```

Mappings: validation/illegal argument → 400; missing auth/invalid token → 401; forbidden
resource → 403; unknown id → 404; duplicate email → 409; unexpected → 500 (no internals
leaked). Covers Requirements 1.3, 2.2, 5.3, 6.3, 8.3, 8.4, 9.5, 9.6, 10.3, 11.4, 13.1.

## Configuration

- `application.yml`: Mongo URI, Redis, JWT secret/expiry, `chargemap.geo.default-radius-km`
  (5), `chargemap.geo.max-results` (50), `chargemap.availability.staleness-minutes`,
  pagination max page size.
- Secrets (JWT, Mongo credentials) come from environment variables, not committed.

## Infrastructure

- **Docker Compose** for local dev: `mongo` (with a named volume), `mongo-express`
  (optional), `redis`. A DB bootstrap/seed step loads ~50–100 sample Cebu stations so
  geo endpoints are demonstrable (product spec section 30).
- The Spring Boot app builds to a runnable jar and its own container.
- Index creation runs on startup (auto-index or explicit `MongoIndexConfig`).

## Testing strategy

- **Unit tests** for services: availability staleness rule, cheapest ordering, report
  update logic, search keyword mapping, cost/compat helpers.
- **Repository/integration tests** with **Testcontainers MongoDB** to exercise real
  `2dsphere` `$geoNear` pipelines (nearby/cheapest/available), text search, and unique
  email index — these cannot be faithfully mocked.
- **Web-layer tests** (`@WebMvcTest` / MockMvc) for status codes, validation errors,
  auth rules (401/403), and DTO shape.
- **Security tests** verifying anonymous reads succeed and protected writes reject
  without a token.

## Mapping to the original PostGIS design

| Product spec (relational + PostGIS) | This design (MongoDB) |
|---|---|
| `station` table | `stations` collection |
| `charger` table (FK station_id) | embedded `chargers[]` with `chargerId` |
| `pricing` table (current + history) | embedded `currentPricing` + `price_history` collection |
| `availability_report` table | `availability_reports` collection |
| `user` table | `users` collection |
| `vehicle` table (FK user_id) | embedded `vehicles[]` under user |
| favorites join table | `favoriteStationIds[]` on user |
| PostGIS `geometry(Point,4326)` | GeoJSON `Point` + `2dsphere` index |
| `ST_DWithin` / `ST_Distance` | `$geoNear` aggregation with `maxDistance` |
| SQL `ORDER BY price` | `$sort` stage on `currentPricing.pricePerKwh` |
| full-text search | MongoDB `$text` index |

All requirements are satisfiable on MongoDB with no PostGIS dependency.
