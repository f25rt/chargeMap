# Design — ChargeMap Community, Contributions & Rewards

## Overview

Extends the existing Spring Boot + MongoDB backend and React/MUI frontend with:
submissions + OCR, moderation, pricing trends, suspension, points, and prizes. Reuses
existing infrastructure (JWT auth, role guards, `Station`/`price_history`, `StationMapper`).

## New/changed data model (MongoDB)

### `users` (extended)
Add fields:
```jsonc
{
  "suspended": false,
  "suspendedReason": null,
  "suspendedAt": null,
  "suspendedBy": null,          // ObjectId of admin/operator
  "pointsBalance": 0,           // current spendable points
  "lifetimePoints": 0,          // never decreases; drives level/title
  "stationsAdded": 0,           // contribution counters
  "updatesMade": 0
}
```

### `stations` (extended)
Add `imageId` (GridFS file id, nullable) so a station can carry a photo.

### `station_submissions` (new collection)
```jsonc
{
  "_id": "ObjectId",
  "submittedBy": "ObjectId",     // user id
  "status": "PENDING",           // PENDING | APPROVED | REJECTED
  "name": "…",
  "operator": "…",
  "address": "…",
  "area": "…",
  "location": { "type": "Point", "coordinates": [lng, lat] },
  "imageId": "ObjectId",         // GridFS
  "ocrText": "…",                // raw OCR text (audit)
  "ocrLooksLikeStation": true,
  "proposedPricePerKwh": "15.00",// Decimal128, may be null
  "connectorType": "CCS2",       // optional proposed charger
  "chargerType": "DC_FAST",
  "powerKw": 60,
  "comments": [                  // embedded thread
    { "authorId": "ObjectId", "authorName": "…", "role": "ADMIN",
      "text": "…", "createdAt": "…" }
  ],
  "reviewedBy": "ObjectId",
  "reviewedAt": "…",
  "createdStationId": "ObjectId",// set when approved
  "createdAt": "…",
  "updatedAt": "…"
}
```
Indexes: `status`, `submittedBy`, `createdAt`.

### `points_ledger` (new collection, append-only)
```jsonc
{
  "_id": "ObjectId",
  "userId": "ObjectId",
  "taskType": "STATION_ADD",     // STATION_ADD | STATION_UPDATE | REPORT | ADMIN_ADJUST
  "points": 50,                  // may be negative for adjustments
  "refId": "ObjectId",           // submission/station/report id (nullable)
  "reason": "…",                 // for manual adjustments
  "createdAt": "…"
}
```
Indexes: `userId` + `createdAt` (compound); used for daily-cap counting and profile feed.

### `prizes` (new collection)
```jsonc
{
  "_id": "ObjectId",
  "name": "…",
  "description": "…",
  "imageId": "ObjectId",         // optional GridFS
  "pointCost": 500,
  "active": true,
  "createdAt": "…",
  "updatedAt": "…"
}
```

### `point_rules` (singleton config document)
```jsonc
{
  "_id": "points-config",
  "pointsPerStationAdd": 50,
  "pointsPerStationUpdate": 10,
  "pointsPerReport": 5,
  "dailyCapStationAdd": 200,
  "dailyCapStationUpdate": 100,
  "dailyCapReport": 50
}
```
Seeded with defaults on startup if absent.

## GridFS image handling

- Use Spring Data MongoDB `GridFsTemplate`.
- `ImageService.store(MultipartFile) -> ObjectId`, `ImageService.load(id) -> resource + contentType`.
- Endpoint `GET /api/images/{id}` streams the file (public; images aren't sensitive).
- Multipart size limits configured in `application.yml` (e.g. 8 MB).

## OCR (client-side, Tesseract.js)

- Runs in the browser on the chosen file before submit. Frontend dependency
  `tesseract.js`.
- After recognition, a `stationTextHeuristic(text)` checks for tokens: `kwh`, `kw`,
  `charging`/`charger`, connector names (`ccs`, `chademo`, `type 2`, `nacs`, `gb/t`),
  currency `₱`/`php`, `dc`/`ac fast`. Returns `looksLikeStation` + a confidence note.
- A `extractPrice(text)` regex finds patterns like `₱\s*\d+(\.\d+)?` or
  `\d+(\.\d+)?\s*(php|pesos)?\s*/?\s*kwh` and returns the first plausible price.
- Results (raw text, looksLikeStation, proposed price) are sent with the submission so
  the server keeps an audit trail. The server does NOT re-run OCR.

## Points engine

`PointsService`:
- `award(userId, taskType, refId)` — reads `point_rules`, checks the day's total for that
  task type from `points_ledger` (entries since local midnight), awards
  `min(rulePoints, remainingDailyCap)`, writes a ledger entry, increments
  `pointsBalance` + `lifetimePoints` and the relevant counter on the user.
- `adjust(userId, delta, reason, actorId)` — admin/operator manual adjustment.
- Contributor level derived from `lifetimePoints`: e.g. 0–99 "Newcomer", 100–499
  "Scout", 500–1999 "Charger Scout", 2000+ "Guardian".

Award triggers:
- Submission APPROVED → `STATION_ADD`.
- Admin/operator marks a user station update accepted, or a user report is accepted →
  `STATION_UPDATE` / `REPORT`. (Reports already exist; wire the award into ReportService.)

## Suspension enforcement

- `suspended` on `User`. Chosen policy: **login still works, contributions blocked**
  (so suspended users can still browse — matches Requirement 5.2 note).
- A small `ContributionGuard` (or a check in each write service via
  `UserService.requireActiveUser()`) throws 403 when `suspended` for: submit station,
  report, add favorite, add/update vehicle, redeem-later actions.
- OPERATOR cannot suspend ADMIN (checked in service).

## API surface

### Public
- `GET /api/images/{id}` — stream image.
- `GET /api/prizes` — active prize catalog (also usable by signed-in users to see cost).

### User (authenticated, active)
- `POST /api/submissions` (multipart: fields + image) — create submission.
- `GET /api/submissions/me` — my submissions + statuses.
- `GET /api/users/me` (extended) — now includes points, level, counters, suspended.
- `GET /api/users/me/points` — ledger feed.

### Moderation (OPERATOR + ADMIN) — `/api/moderation/**`
- `GET /api/moderation/submissions?status=` — list.
- `GET /api/moderation/submissions/{id}` — detail.
- `POST /api/moderation/submissions/{id}/comment` — add comment.
- `POST /api/moderation/submissions/{id}/approve` — approve → create station + award.
- `POST /api/moderation/submissions/{id}/reject` — reject (reason required).
- `PUT /api/moderation/stations/{id}/location` — edit location.
- `PUT /api/moderation/stations/{id}/image` (multipart) — replace image.
- (pricing edit reuses existing `/api/operator/stations/{id}/pricing`.)

### User management (OPERATOR + ADMIN) — under `/api/moderation/users`
- `GET /api/moderation/users` — list users (role, suspended, points, counts).
- `POST /api/moderation/users/{id}/suspend` (reason) / `.../unsuspend`.
  (Service rejects operator-suspends-admin.)

### Points/prizes management (OPERATOR + ADMIN)
- `GET/PUT /api/moderation/point-rules`.
- `POST /api/moderation/prizes`, `PUT /api/moderation/prizes/{id}`,
  `POST /api/moderation/prizes/{id}/deactivate`.
- `POST /api/moderation/users/{id}/points` — manual adjust (delta + reason).

### Admin only — `/api/admin/**`
- `GET /api/admin/pricing-trends?days=90` — stations ranked by price-change count.

Note: `/api/moderation/**` is added to SecurityConfig with
`hasAnyRole("OPERATOR","ADMIN")`.

## Frontend design

- **Consumer:** add a "＋ Add station" flow (FAB or menu) → a form/dialog with image
  picker (runs Tesseract.js, shows detected price for confirm/edit), map-pin/geolocation
  capture, submit. A "My contributions" view showing submission statuses + points +
  prize catalog preview on the profile.
- **Operator/Admin dashboards:** add tabs/sections —
  - Submissions queue (approve/reject/comment, view image).
  - Users (suspend/unsuspend, adjust points).
  - Point rules + Prizes management.
  - Admin also: Pricing trends chart/list.
- **Responsive:** dashboards use a responsive `Drawer` (permanent on `md+`, temporary +
  hamburger on mobile) or a tab bar; content grids collapse to one column.

## Testing strategy

- Backend: unit tests for `PointsService` (daily cap, award, adjust) and suspension
  rules; Testcontainers IT for submission→approve→station-created + points awarded, and
  pricing-trends aggregation.
- Frontend: build must stay clean; manual E2E of the submission + moderation + points
  flows per role.
- OCR is client-only and hard to unit test headlessly; the price-regex + heuristic are
  pure functions and will get small unit tests.
