# Implementation Plan — ChargeMap Community, Contributions & Rewards

Phased build. Each phase ends with a build + verification step. Requirement refs point
to `requirements.md`.

## Phase 1 — Suspension + pricing trends

- [ ] 1.1 Extend `User` with suspension fields + points/counter fields; extend
  `UserProfileDto` and `UserService.requireActiveUser()`.
  - _Requirements: 5, 6.3_
- [ ] 1.2 Suspension endpoints under `/api/moderation/users` (list, suspend, unsuspend);
  block suspend-admin-by-operator; enforce contribution block in report service.
  - _Requirements: 5.1–5.6_
- [ ] 1.3 Add `/api/moderation/**` role guard (OPERATOR+ADMIN) to SecurityConfig.
  - _Requirements: 9.3_
- [ ] 1.4 Admin pricing-trends endpoint (aggregate `price_history` by station, window).
  - _Requirements: 4_
- [ ] 1.5 Frontend: Admin "Pricing trends" section; Admin/Operator "Users" section with
  suspend/unsuspend.
  - _Requirements: 4, 5_
- [ ] 1.6 Verify: backend build + tests, frontend build, role flows.

## Phase 2 — Submissions + OCR + moderation + station editing

- [ ] 2.1 GridFS `ImageService` + `GET /api/images/{id}`; multipart config.
  - _Requirements: 1.4, 9.2_
- [ ] 2.2 `StationSubmission` document + repository; `SubmissionService.create`
  (multipart) storing image + OCR fields; block suspended users.
  - _Requirements: 1.1–1.6_
- [ ] 2.3 Moderation endpoints: list/detail/comment/approve/reject; approve creates a
  `Station` (with imageId) and awards STATION_ADD points.
  - _Requirements: 2.1–2.5, 6.1_
- [ ] 2.4 Station editing: location + image replace under `/api/moderation/stations/{id}`.
  - _Requirements: 3.1–3.3_
- [ ] 2.5 Frontend consumer: "Add station" flow with Tesseract.js OCR (price auto-fill,
  station heuristic warning), geolocation, image preview; "My contributions" list.
  - _Requirements: 1.1–1.3_
- [ ] 2.6 Frontend moderation: submissions queue with image, comments, approve/reject.
  - _Requirements: 2_
- [ ] 2.7 Verify.

## Phase 3 — Points + prizes + management

- [ ] 3.1 `points_ledger`, `point_rules` (seeded), `prizes` collections + repositories.
  - _Requirements: 6, 7_
- [ ] 3.2 `PointsService` (award w/ daily cap, adjust) + wire STATION_ADD (approve),
  STATION_UPDATE/REPORT (report accept).
  - _Requirements: 6.1–6.5_
- [ ] 3.3 Prize catalog endpoints (`GET /api/prizes`) + management
  (`/api/moderation/prizes`, `/api/moderation/point-rules`,
  `/api/moderation/users/{id}/points`).
  - _Requirements: 7.1–7.6_
- [ ] 3.4 Frontend: profile points + level + ledger; prize catalog (display-only);
  admin/operator management of rules/prizes/manual adjust.
  - _Requirements: 6.4, 7_
- [ ] 3.5 Verify.

## Phase 4 — Responsive admin/operator polish

- [ ] 4.1 Responsive dashboard shell (permanent drawer on md+, temporary + hamburger on
  mobile; content grids collapse to one column; safe-area).
  - _Requirements: 8.1, 8.2_
- [ ] 4.2 Verify on mobile + desktop widths.
