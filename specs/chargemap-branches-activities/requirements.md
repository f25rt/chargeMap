# Branches, Super Admin, Metrics & Task Activities — Requirements

## Introduction

Extends ChargeMap with an organizational layer (branches), a super-admin role that
oversees admins, admin self-service, richer metrics, and a user "task activity" system
that grants rewards on completion. Builds on the existing Spring Boot + MongoDB backend
and React/MUI frontend, and the existing USER / OPERATOR / ADMIN roles + JWT auth.

## Roles (updated)

- **USER** — consumer (unchanged).
- **OPERATOR** — station operator (unchanged).
- **ADMIN** — belongs to (is assigned to) a **branch**; moderates within the app.
- **SUPER_ADMIN** — new top role. Oversees all admins and branches. A SUPER_ADMIN also
  satisfies every ADMIN-guarded route (implemented by granting both authorities).

## Requirements

### R1 — Branch management (SUPER_ADMIN)
1. SUPER_ADMIN SHALL create/edit/list branches (name, description, area).
2. A branch SHALL support a **profile image** and a **banner image** (uploaded, stored in GridFS).
3. SUPER_ADMIN SHALL assign admins to a branch and assign stations to a branch.
4. A branch MAY define a marker **icon** (its profile image) used for its stations on the map.

### R2 — Super admin oversight
1. SUPER_ADMIN SHALL list all admins with their assigned branch.
2. SUPER_ADMIN SHALL change an admin's assigned branch.
3. SUPER_ADMIN SHALL disable/enable an admin (a disabled admin cannot act).

### R3 — Admin self-profile
1. An admin SHALL view their profile (name, email, assigned branch).
2. An admin SHALL edit their name and change their password (with current-password check).

### R4 — Metrics / studies
1. **Branch price trend** — price-change counts over a selectable window (day/week), per branch.
2. **Per-station update frequency** — how often users submit new/updated prices and images
   for a specific station (approved edits count).
3. **Rewards completion** — which reward/activity is completed most by users.

### R5 — Task activities (users) tied to rewards
1. An admin/super-admin SHALL create an **activity**: a title, a goal (e.g. "update prices +
   upload image on N stations, approved"), and a **linked reward** granted on completion.
2. A user SHALL browse available activities and **choose** one; the chosen activity becomes
   **IN_PROGRESS** for that user.
3. As the user's qualifying approved contributions accrue, the activity's progress SHALL
   advance; WHEN the goal is met the activity SHALL be **COMPLETED** and the linked reward
   **granted** to the user (recorded), enabling the "most-completed reward" metric.
4. A user SHALL see progress (e.g. 2/3) for an in-progress activity.

### R6 — Map icons (already delivered in the UI batch)
- Current position = car icon; station markers use the branch icon if set, else a default
  EV-station glyph.

## Build order (implemented)
1. SUPER_ADMIN role + security (dual authority) + seeded super admin.
2. Branch entity + CRUD + image upload + assign admins/stations.
3. Super-admin oversight (reassign branch, disable admin).
4. Admin self-profile (edit name, change password).
5. Metrics endpoints (branch price trend, station update frequency, reward completion).
6. Activities + user activity progress + reward grant on completion.
7. Frontend surfaces for each.
