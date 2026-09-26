# Deploying ChargeMap to Render (Free Tier, Manual)

You can deploy the whole app on Render's **free tier** by creating the services **by hand**
(Blueprints may require a paid account). No Redis is needed — rate limiting is optional and
disabled automatically when Redis is absent. MongoDB uses **Atlas free tier (M0)**.

You create two services:

1. **chargemap-api** — a free **Web Service** built from the backend `Dockerfile`
2. **chargemap-web** — a free **Static Site** (the Vite build) that rewrites `/api` to the
   backend so the app stays same-origin (no CORS setup)

> There is intentionally **no `render.yaml`** in the repo. A Blueprint file was removed
> because (a) Blueprints may need a paid plan and (b) Render was mis-reading it. Create the
> two services **manually** as described below.

### If you already saw: `render.yaml:11 >>> services:` (Dockerfile parse error)

That means your backend Web Service has its **Dockerfile Path set to `render.yaml`** (Render
tried to parse the YAML as a Dockerfile — `services:` isn't a Docker instruction). **Fix:**
open the service → **Settings** → set **Dockerfile Path = `Dockerfile`** → Save → Manual
Deploy. (Deleting `render.yaml` from the repo alone won't fix a service that's already
pointed at it — you must correct the path.)

---

## 0. Prerequisites

- Repo pushed to GitHub/GitLab (Render deploys from a connected repo).
- **MongoDB Atlas** free cluster + connection string. In Atlas → Network Access, add
  `0.0.0.0/0` (Render's egress IPs aren't static on free tier).
- A **Mapbox** publishable token.
- Two secrets you generate locally:
  - `openssl rand -base64 48`  → JWT secret
  - `openssl rand -base64 32`  → crypto key (must decode to exactly 32 bytes)

---

## 1. Backend — Web Service (Docker, free)

Render Dashboard → **New +** → **Web Service** → connect your repo. Then set:

- **Name:** `chargemap-api`
- **Language / Runtime:** **Docker**
- **Dockerfile Path:** `Dockerfile`   ← the root one
- **Docker Build Context Directory:** `.` (repo root)
- **Root Directory:** *(leave blank / repo root)*
- **Instance Type:** **Free**
- **Health Check Path:** `/actuator/health`

> If Render tries to auto-detect docker-compose or the wrong Dockerfile, explicitly setting
> **Dockerfile Path = `Dockerfile`** fixes it. There is no bare `docker-compose.yml` in the
> repo (it's `docker-compose.dev.yml` / `docker-compose.prod.yml`), so it won't be picked up.

**Environment variables** (Advanced → Add Environment Variable):

| Key | Value |
|-----|-------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SERVER_PORT` | `8080` |
| `MONGODB_URI` | your Atlas SRV string with `/chargemap` before the `?` |
| `JWT_SECRET` | the `openssl rand -base64 48` value |
| `JWT_EXPIRY_MINUTES` | `1440` |
| `CHARGEMAP_CRYPTO_KEY` | the `openssl rand -base64 32` value |
| `CHARGEMAP_SUPERADMIN_EMAIL` | your super-admin login email |
| `CHARGEMAP_SUPERADMIN_PASSWORD` | a strong password |

Create the service. First build takes a few minutes (Maven + Vite). When live, note its URL
(e.g. `https://chargemap-api.onrender.com`) and verify:

```
https://chargemap-api.onrender.com/actuator/health   →  {"status":"UP"}
```

On first boot the backend creates **one SUPER_ADMIN** (from the two env vars) and nothing
else — stations, rewards, and other users start empty.

---

## 2. Frontend — Static Site (free)

Render Dashboard → **New +** → **Static Site** → same repo. Set:

- **Name:** `chargemap-web`
- **Build Command:** `cd frontend && npm ci && npm run build`
- **Publish Directory:** `frontend/dist`
- **Environment variable:** `VITE_MAPBOX_TOKEN` = your Mapbox token
  (leave `VITE_API_BASE_URL` unset/empty — the rewrite below handles the API)

**Redirects/Rewrites** (Settings → Redirects/Rewrites) — add these, in order:

| Source | Destination | Action |
|--------|-------------|--------|
| `/api/*` | `https://chargemap-api.onrender.com/api/*` | **Rewrite** |
| `/actuator/*` | `https://chargemap-api.onrender.com/actuator/*` | **Rewrite** |
| `/*` | `/index.html` | **Rewrite** |

> Replace `chargemap-api.onrender.com` with your **actual backend URL** from step 1.
> The first two keep the app same-origin (so no CORS); the last is the SPA fallback.

Create the site. When it's live, open its URL — the app loads and talks to the backend
through the rewrite.

---

## 3. Free-tier behavior to expect

- The backend **spins down after ~15 min idle**; the next request cold-starts it (a Spring
  Boot jar takes ~30–60s to wake). The first hit after idle will be slow, and the health
  check may briefly flap. This is normal on free tier.
- The static site does not sleep.
- MongoDB Atlas M0 is always-on.

---

## 4. Updating

Both services have auto-deploy on by default — push to the connected branch and Render
rebuilds. Otherwise use **Manual Deploy** in each service.

---

## 5. Troubleshooting

- **"docker-compose mongo image" error / wrong Dockerfile:** you created the backend
  without setting **Dockerfile Path = `Dockerfile`**, or used a Blueprint. Set it explicitly
  (step 1) and redeploy.
- **Backend fails to start with a crypto error:** `CHARGEMAP_CRYPTO_KEY` must be Base64 that
  decodes to exactly 32 bytes — regenerate with `openssl rand -base64 32`.
- **Frontend loads but API calls 404/blocked:** the rewrite destination host doesn't match
  the real backend URL — fix the two rewrites in step 2.
- **Cannot connect to Mongo:** ensure Atlas Network Access allows `0.0.0.0/0` and the URI
  has the `/chargemap` database name.

---

## Checklist

- [ ] Backend Web Service: Docker, Dockerfile Path `Dockerfile`, Free, health `/actuator/health`.
- [ ] Backend env: `MONGODB_URI`, `JWT_SECRET`, `CHARGEMAP_CRYPTO_KEY`, super-admin email+password, `SPRING_PROFILES_ACTIVE=prod`.
- [ ] Static Site: build `cd frontend && npm ci && npm run build`, publish `frontend/dist`, `VITE_MAPBOX_TOKEN` set.
- [ ] Three rewrites added; `/api/*` + `/actuator/*` point at the real backend URL.
- [ ] Atlas Network Access allows `0.0.0.0/0`; URI has `/chargemap`.
- [ ] `https://<web-url>/actuator/health` returns UP through the rewrite.
