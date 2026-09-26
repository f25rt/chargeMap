# Deploying ChargeMap to a DigitalOcean Droplet

This deploys the full stack with Docker Compose on a single droplet:

- **backend** — Spring Boot fat jar (Java 21), internal only
- **redis** — rate limiting
- **frontend** — Nginx serving the built SPA and reverse-proxying `/api` + `/actuator` to the backend
- **MongoDB** — MongoDB Atlas (managed, recommended)

The frontend is the only container that exposes a public port (80). Everything else talks
over the private Compose network. HTTPS is added with Caddy (Section 6) or Certbot.

---

## 0. Prerequisites

- A **MongoDB Atlas** cluster + connection string (see `DEPLOYMENT.md` §3). Allow the
  droplet's IP in Atlas → Network Access.
- A **Mapbox token** (`VITE_MAPBOX_TOKEN`).
- A domain name (optional but needed for HTTPS), e.g. `chargemap.example.com`.
- The repo pushed to GitHub (or copied to the droplet).

---

## 1. Create the droplet

- **Image:** Ubuntu 24.04 LTS
- **Plan:** Basic — **2 GB RAM / 1 vCPU minimum** (the Maven build + JVM need headroom;
  4 GB is comfortable). Add swap (Section 2) if you pick 2 GB.
- **Auth:** SSH key.
- **Region:** closest to your users (e.g. Singapore `sgp1` for PH).

SSH in:

```bash
ssh root@YOUR_DROPLET_IP
```

---

## 2. Base setup (Docker + firewall + swap)

```bash
# System update
apt-get update && apt-get upgrade -y

# Docker Engine + Compose plugin (official convenience script)
curl -fsSL https://get.docker.com | sh
docker compose version   # verify the compose plugin is present

# Firewall: allow SSH + HTTP + HTTPS only
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable

# Swap (recommended on 2 GB droplets so the build doesn't OOM)
fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
```

> Optional: create a non-root sudo user and deploy as that user instead of root.

---

## 3. Get the code onto the droplet

```bash
git clone https://github.com/<you>/chargeMap.git
cd chargeMap
```

(Or `scp -r` the project up if it's not on GitHub.)

---

## 4. Configure secrets

```bash
cp .env.production.example .env.production
nano .env.production
```

Fill in:

- `MONGODB_URI` — the Atlas SRV string with `/chargemap` before the `?`.
- `JWT_SECRET` — `openssl rand -base64 48`
- `CHARGEMAP_CRYPTO_KEY` — `openssl rand -base64 32` (must decode to exactly 32 bytes)
- `VITE_MAPBOX_TOKEN` — your Mapbox token.

The Mapbox token is a **build-time** value for the frontend, so also export it in the shell
before building (Compose reads `${VITE_MAPBOX_TOKEN}` for the build arg):

```bash
export VITE_MAPBOX_TOKEN="$(grep -E '^VITE_MAPBOX_TOKEN=' .env.production | cut -d= -f2-)"
```

---

## 5. Build and run

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

First build takes several minutes (Maven downloads deps, Vite builds). Check status:

```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f backend      # watch startup
```

Verify:

```bash
curl -s http://localhost/actuator/health      # {"status":"UP"} via the Nginx proxy
curl -s "http://localhost/api/stations/nearby?lat=10.3181&lng=123.9068&radius=10" | head
```

Then browse to `http://YOUR_DROPLET_IP`.

> The production backend runs with `SPRING_PROFILES_ACTIVE=prod`, so the demo seeders do
> NOT run — production starts with an empty database. Create real accounts/stations, or
> seed intentionally (see "Seeding production" below).

---

## 6. HTTPS with Caddy (recommended, automatic certs)

Point your domain's A record at the droplet IP first. Then add a Caddy service that
terminates TLS and proxies to the frontend container. Create `Caddyfile`:

```
chargemap.example.com {
    reverse_proxy frontend:80
}
```

Add to `docker-compose.prod.yml` (and change the frontend to no longer publish port 80):

```yaml
  caddy:
    image: caddy:2
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - caddy-data:/data
      - caddy-config:/config
    depends_on:
      - frontend
    networks:
      - chargemap
# ...and under `volumes:` add  caddy-data:  and  caddy-config:
# ...and remove the `ports: - "80:80"` mapping from the frontend service.
```

Re-deploy: `docker compose -f docker-compose.prod.yml up -d`. Caddy fetches a Let's Encrypt
cert automatically. Your app is now at `https://chargemap.example.com`.

> Alternative: run Nginx/Certbot on the host instead of Caddy — Caddy is simpler for
> automatic renewal.

---

## 7. Updating the app

```bash
cd chargeMap
git pull
export VITE_MAPBOX_TOKEN="$(grep -E '^VITE_MAPBOX_TOKEN=' .env.production | cut -d= -f2-)"
docker compose -f docker-compose.prod.yml up -d --build
docker image prune -f          # reclaim old image layers
```

---

## 8. Seeding production (optional)

Production does not auto-seed. To load the demo dataset once (10 stations, 5 prizes, demo
accounts), run the backend container once with the dev profile + reseed flag, then revert:

```bash
docker compose -f docker-compose.prod.yml run --rm \
  -e SPRING_PROFILES_ACTIVE=dev -e CHARGEMAP_RESEED=true backend \
  sh -c 'java -jar app.jar'    # Ctrl-C after "Seeded ... stations" in the logs
```

For a real launch, prefer creating genuine data instead.

---

## 9. Operations cheat-sheet

```bash
# Logs
docker compose -f docker-compose.prod.yml logs -f backend
docker compose -f docker-compose.prod.yml logs -f frontend

# Restart one service
docker compose -f docker-compose.prod.yml restart backend

# Stop / start the whole stack
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml up -d

# Resource usage
docker stats
```

---

## 10. Production checklist

- [ ] `MONGODB_URI` → Atlas, with `/chargemap` db name; droplet IP allow-listed in Atlas.
- [ ] `JWT_SECRET` and `CHARGEMAP_CRYPTO_KEY` are strong, generated values (not the samples).
- [ ] `VITE_MAPBOX_TOKEN` set and exported before build.
- [ ] Firewall (`ufw`) allows only SSH/80/443.
- [ ] HTTPS via Caddy (or Certbot); A record points at the droplet.
- [ ] `SPRING_PROFILES_ACTIVE=prod` (no seeders); `CHARGEMAP_RESEED` unset.
- [ ] Backend port 8080 is NOT published publicly (only the frontend/Caddy is).
- [ ] Regular Atlas backups enabled.
