# Requirements — ChargeMap Community, Contributions & Rewards

## Introduction

This spec extends the ChargeMap PH backend + web app with community contribution,
moderation, gamification, and analytics features. It builds on the existing MongoDB
backend (Spring Boot) and the React/MUI web frontend.

### Confirmed technology decisions

- **OCR:** client-side **Tesseract.js** for text extraction from an uploaded station
  photo. A **keyword heuristic** over the extracted text decides whether the image
  plausibly shows a charging station (looks for "kWh", "charging", connector names,
  "₱", "DC"/"AC", etc.) and extracts a price list for auto-fill. True image
  classification (recognizing a charger from pixels) is out of scope for this phase.
- **Image storage:** **MongoDB GridFS** (no external bucket). Images are served back
  through an API endpoint.
- **Prizes:** catalog + points balances + admin/operator management now. **No actual
  redemption / checkout** in this phase — prizes are displayed as "purchasable in the
  future".
- **Roles:** existing USER / OPERATOR / ADMIN.

### Build phases

- **Phase 1:** user suspension + pricing-trend analytics.
- **Phase 2:** station submission workflow (image upload + geolocation + OCR auto-fill)
  and admin/operator approval + comments + station editing.
- **Phase 3:** points system + prize catalog + management.
- **Phase 4:** responsive polish for admin/operator dashboards.

---

## Requirements

### Requirement 1 — User submits a new station (with image + geolocation + OCR)

**User Story:** As a signed-in user, I want to submit a new charging station with a
photo and its location, so that the community map grows and I earn points.

#### Acceptance Criteria

1. WHEN a signed-in user submits a station with name, location (lat/lng), an image, and
   optional details THEN the system SHALL create a `StationSubmission` in `PENDING`
   status linked to that user.
2. WHEN the image is uploaded THEN the client SHALL run Tesseract.js OCR and, IF the
   extracted text matches the charging-station keyword heuristic, allow the submission;
   IF it does not match THEN the client SHALL warn the user but still allow manual
   override.
3. WHEN the OCR detects price information (e.g. "₱15/kWh") THEN the client SHALL
   auto-fill the price field; the user MAY edit or enter pricing manually.
4. WHEN a submission is stored THEN the image SHALL be persisted in GridFS and
   retrievable via an image endpoint.
5. WHEN an unauthenticated request attempts to submit THEN the system SHALL respond 401.
6. WHEN a suspended user attempts to submit THEN the system SHALL respond 403.

### Requirement 2 — Admin/operator review submissions (approve / reject / comment)

**User Story:** As an admin or operator, I want to review user submissions, comment on
them, and approve or reject, so that only good data reaches the live map.

#### Acceptance Criteria

1. WHEN an admin/operator lists submissions THEN the system SHALL return them filterable
   by status (PENDING, APPROVED, REJECTED) with submitter, image, and detected pricing.
2. WHEN an admin/operator approves a submission THEN the system SHALL create a live
   `Station` from it, mark the submission APPROVED, and award points to the submitter.
3. WHEN an admin/operator rejects a submission THEN the system SHALL mark it REJECTED
   with a required reason/comment and award no points.
4. WHEN an admin/operator adds a comment THEN the system SHALL append it to the
   submission's comment thread with author and timestamp.
5. WHEN a non-admin/operator accesses review endpoints THEN the system SHALL respond 403.

### Requirement 3 — Admin/operator edit stations (location, image, price)

**User Story:** As an admin or operator, I want to edit a station's location, image,
and price, so that I can correct data.

#### Acceptance Criteria

1. WHEN an admin/operator updates a station's location THEN the system SHALL update the
   GeoJSON point and `lastUpdated`.
2. WHEN they replace the station image THEN the new image SHALL be stored in GridFS and
   referenced by the station.
3. WHEN they update the price THEN the previous price SHALL be archived to
   `price_history` (reusing existing pricing update behavior).

### Requirement 4 — Pricing-trend analytics (admin)

**User Story:** As an admin, I want to see which stations change price most often, so
that I can spot volatile pricing.

#### Acceptance Criteria

1. WHEN an admin requests pricing trends THEN the system SHALL return stations ranked by
   number of price changes (from `price_history`) within a configurable window
   (default 90 days), including change count, current price, and last change date.
2. WHEN a station has no price history THEN it SHALL be excluded or shown with zero
   changes.
3. WHEN a non-admin accesses the trends endpoint THEN the system SHALL respond 403.

### Requirement 5 — User suspension (admin/operator)

**User Story:** As an admin or operator, I want to suspend abusive users, so that they
cannot contribute until reviewed.

#### Acceptance Criteria

1. WHEN an admin/operator suspends a user THEN the system SHALL set the user's
   `suspended` flag and record who/when/why.
2. WHEN a suspended user attempts to log in THEN the system SHALL respond 403 with a
   clear message; OR login succeeds but all contribution endpoints (submit, report,
   favorites-write, vehicle-write) respond 403 while suspended. (This spec uses the
   block-contributions approach so suspended users can still browse.)
3. WHEN an admin/operator unsuspends a user THEN the system SHALL clear the flag.
4. WHEN an admin/operator lists users THEN the system SHALL return users with role,
   suspended state, points, and contribution counts.
5. WHEN a non-admin/operator accesses user-management endpoints THEN respond 403.
6. An OPERATOR SHALL NOT be able to suspend an ADMIN.

### Requirement 6 — Points system

**User Story:** As a user, I want to earn points for contributing, so that I'm rewarded
and can eventually redeem prizes.

#### Acceptance Criteria

1. WHEN a user's submission is approved THEN the system SHALL award the configured
   points for "STATION_ADD" and record a `PointsLedger` entry.
2. WHEN a user's station update/report is accepted THEN the system SHALL award the
   configured points for that task type, subject to a **daily cap** per task type.
3. WHEN points are awarded THEN the user's `pointsBalance` SHALL increase and be visible
   on their profile.
4. WHEN a user views their profile THEN the system SHALL return points balance, a
   contributor level/title derived from lifetime points, and recent ledger entries.
5. WHEN points would exceed the daily cap for a task type THEN the system SHALL award 0
   for the additional actions that day.

### Requirement 7 — Prizes catalog + points/prize management

**User Story:** As a user, I want to see prizes I could get with my points; as an
admin/operator, I want to manage point rules and prizes.

#### Acceptance Criteria

1. WHEN a user views prizes THEN the system SHALL return the active prize catalog with
   name, description, image, and point cost, plus the user's current balance.
2. WHEN an admin/operator creates/updates/deactivates a prize THEN the catalog SHALL
   reflect it.
3. WHEN an admin/operator updates point rules (points per task type, daily caps) THEN
   subsequent awards SHALL use the new values.
4. WHEN an admin/operator manually adjusts a user's points THEN a ledger entry SHALL be
   recorded with the actor and reason.
5. Redemption/checkout is explicitly out of scope; prizes are display-only for now.
6. WHEN a non-admin/operator accesses management endpoints THEN respond 403.

### Requirement 8 — Responsive admin/operator views

**User Story:** As an admin or operator on a phone, I want the dashboards to be usable,
so that I can moderate on the go.

#### Acceptance Criteria

1. WHEN the admin or operator dashboard is viewed below the `md` breakpoint THEN the
   layout SHALL adapt to a single-column, touch-friendly form with a navigation drawer
   or bottom navigation, no horizontal overflow, and safe-area insets respected.
2. WHEN viewed at `md` and above THEN it SHALL use a multi-column dashboard layout.

### Requirement 9 — Cross-cutting

1. All new write endpoints SHALL validate input and return the consistent error body.
2. Image endpoints SHALL set appropriate content types and reasonable size limits.
3. Role protection SHALL reuse the existing `/api/admin/**` and `/api/operator/**`
   guards; shared review actions available to both roles SHALL be under a path both can
   reach (e.g. `/api/moderation/**` allowing OPERATOR + ADMIN).
