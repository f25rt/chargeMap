# ChargeMap — Deployment & Run Guide

ChargeMap is a two-part app:

- **Backend** — Java 21 / Spring Boot, talks to **MongoDB** (Atlas in the cloud) and **Redis** (rate limiting). Serves a REST API under `/api` and health under `/actuator/health`.
- **Frontend** — React + Vite (MUI). Calls the backend over same-origin `/api` (proxied in dev, reverse-proxied in prod).

---

## 1. Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| JDK | 21 | Backend build/run (Eclipse Temurin used in dev) |
| Maven | 3.9+ | Backend build (`mvn`) |
| Node.js | 20+ | Frontend build/run |
| Redis | 7 | Rate limiting; local Docker in dev |
| MongoDB | 7 / Atlas | Local Docker in dev, Atlas in prod |

---

## 2. Configuration (environment variables)

The backend reads **everything from environment variables** with dev-friendly fallbacks
(see `src/main/resources/application.yml`). Nothing sensitive is hardcoded.

| Variable | Required in prod | Default (dev) | Purpose |
|----------|------------------|---------------|---------|
| `MONGODB_URI` | ✅ | `mongodb://localhost:27017/chargemap` | MongoDB connection string |
| `REDIS_HOST` | ✅ | `localhost` | Redis host |
| `REDIS_PORT` | | `6379` | Redis port |
| `JWT_SECRET` | ✅ | dev-only placeholder | Base64 HMAC secret for signing tokens |
| `JWT_EXPIRY_MINUTES` | | `1440` | Access-token lifetime |
| `SERVER_PORT` | | `8080` | HTTP port |
| `SPRING_PROFILES_ACTIVE` | | (none) | Set to `dev` to enable demo seeders |
| `CHARGEMAP_RESEED` | | `false` | **Dev only.** `true` wipes + reseeds demo data on startup |

Local values live in a **gitignored `.env`** file (copy from `.env.example`). Spring Boot
does not read `.env` automatically — the run commands below load it into the environment first.

> **Generate a production JWT secret:** `openssl rand -base64 48`

---

## 3. MongoDB Atlas

The production database is MongoDB Atlas.

1. In Atlas: **Database Access** → create a DB user (username + password).
2. **Network Access** → allow your server's IP (or `0.0.0.0/0` for testing only — tighten for prod).
3. **Connect → Drivers** → copy the connection string. It looks like:

   ```
   mongodb+srv://<user>:<password>@<cluster>.mongodb.net/?appName=ChargeMap
   ```

4. **Insert the database name** (`chargemap`) before the `?`, and keep `retryWrites`:

   ```
   mongodb+srv://<user>:<password>@<cluster>.mongodb.net/chargemap?retryWrites=true&w=majority&appName=ChargeMap
   ```

5. Put that value in `MONGODB_URI` (in `.env` locally, or the platform's secret manager in prod).

> Atlas clusters are replica sets, which means multi-document **transactions** are available
> if/when the app adopts them.

**Security:** never commit real credentials. `.env` is gitignored. If a credential is ever
exposed, rotate it in **Atlas → Database Access → Edit → Edit Password** and update `MONGODB_URI`.

---

## 4. Running locally

### Backend (against local Docker Mongo + Redis)

```powershell
# From the repo root (dev stack: local Mongo + Redis)
docker compose -f docker-compose.dev.yml up -d   # starts chargemap-mongo + chargemap-redis
$env:JAVA_HOME = "C:\path\to\jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:SPRING_PROFILES_ACTIVE = "dev"
mvn -q spring-boot:run
```

### Backend (against Atlas, loading `.env`)

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
# Load .env into the process environment:
Get-Content .env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
  $k,$v = $_ -split '=',2; Set-Item -Path "env:$($k.Trim())" -Value $v.Trim()
}
$env:SPRING_PROFILES_ACTIVE = "dev"
mvn -q spring-boot:run
```

Check health: `http://localhost:8080/actuator/health` → `{"status":"UP"}`.

### Frontend

```powershell
cd frontend
npm install
npm run dev        # http://localhost:5173, proxies /api -> :8080
```

---

## 5. Demo data seeding (dev profile only)

When `SPRING_PROFILES_ACTIVE=dev`, three seeders run on startup:

- **`UserSeeder`** — ensures demo accounts exist (idempotent; never resets passwords):
  - `admin@chargemap.ph` (ADMIN)
  - `user@chargemap.ph` (USER)
  - `maria@chargemap.ph` (USER)
  - `operator@chargemap.ph` (OPERATOR)
  - Password for all: `password123`
- **`StationSeeder`** — seeds **10** Metro Cebu stations when the collection is empty.
- **`PrizeSeeder`** — seeds **5** demo rewards when the collection is empty.

The station/prize seeders only run on an **empty** collection, so a normal restart preserves data.

### Wipe & reseed on purpose

To reset demo data to the target counts (e.g. after experimenting), start **once** with the
reseed flag, then restart normally:

```powershell
$env:CHARGEMAP_RESEED = "true"    # in addition to the normal env above
mvn -q spring-boot:run
# ...confirm counts, then stop and restart WITHOUT the flag
```

`DemoDataReseeder` drops the demo collections (`stations`, `prizes`, `users`,
`station_submissions`, `points_ledger`, `reports`, `favorites`) **before** the seeders run,
then the seeders repopulate. With the flag unset it does nothing — so it is safe by default.

> `CHARGEMAP_RESEED` is dev-only and destructive. Never set it in production.

---

## 6. Production build

### Backend

```bash
mvn -q clean package        # produces target/chargemap-*.jar
java -jar target/chargemap-*.jar   # with prod env vars exported (no `dev` profile)
```

Without the `dev` profile, the seeders do not run and no demo data is created.

### Frontend

```bash
cd frontend
npm ci
npm run build               # outputs static assets to frontend/dist
```

Serve `frontend/dist` behind a web server / CDN, and make sure requests to `/api` and
`/actuator` reach the backend (same origin via a reverse proxy such as Nginx, or your
host's routing). The frontend uses **same-origin** requests — there is no hardcoded API host.

Example Nginx sketch:

```nginx
location /api/       { proxy_pass http://backend:8080; }
location /actuator/  { proxy_pass http://backend:8080; }
location /           { root /var/www/chargemap/dist; try_files $uri /index.html; }
```

---

## 7. Production checklist

- [ ] `MONGODB_URI` points at Atlas with the `chargemap` DB name and a rotated password.
- [ ] Atlas Network Access restricted to the server IP (not `0.0.0.0/0`).
- [ ] `JWT_SECRET` set to a strong `openssl rand -base64 48` value (not the dev default).
- [ ] `REDIS_HOST` / `REDIS_PORT` point at a real Redis.
- [ ] `SPRING_PROFILES_ACTIVE` is **not** `dev` (no seeders in prod).
- [ ] `CHARGEMAP_RESEED` is **unset**.
- [ ] Frontend built with `npm run build` and served with `/api` reverse-proxied to the backend.
- [ ] HTTPS terminated at the proxy/CDN.
