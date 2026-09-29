# Importing real EV charging stations from OpenStreetMap

ChargeMap can seed its `stations` collection with **real** EV charging stations pulled
from OpenStreetMap (OSM) via the Overpass API. This gets you real coverage on day one
instead of an empty map, and your community features (availability reports, prices,
reviews, points) build on top of it.

## What it does

- Queries OSM for `amenity=charging_station` within a bounding box.
- Maps OSM tags to your `Station` model (name, operator, address, connectors from
  `socket:*`, power, coordinates).
- Marks each imported station:
  - `dataSource = OPEN_DATASET`
  - `confidence = LOW` (community edits refine it)
  - `availabilitySummary = UNKNOWN`, no fabricated availability
  - `currentPricing = null` (OSM has no reliable price)
  - `sourceRef = "osm:node/<id>"` (or `way`/`relation`) for dedup
- **Idempotent:** re-running matches on `sourceRef` and updates the OSM facts only. It
  never overwrites community data — availability, price, rating, likes, image, branch,
  or the disabled flag are all preserved.
- Failures are logged and swallowed, so a bad import never crashes the app.

## Licensing / attribution (required)

OSM data is licensed under the **Open Database License (ODbL)**. Attribution to
"OpenStreetMap contributors" is already shown on the map (see `StationMap.tsx`
attribution control). Keep it. Do not remove the attribution if you redistribute.

## How to run it

The import is **opt-in** and triggered on startup by a config flag. Turn it on, start
the app once to seed, then turn it off for normal runs.

### Configuration

| Env var | application.yml key | Default | Meaning |
| ------- | ------------------- | ------- | ------- |
| `CHARGEMAP_IMPORT_OSM` | `chargemap.import.osm.enabled` | `false` | Run import on startup |
| `CHARGEMAP_IMPORT_OSM_SOUTH` | `...bbox.south` | `4.5` | Bounding box south latitude |
| `CHARGEMAP_IMPORT_OSM_WEST` | `...bbox.west` | `116.0` | west longitude |
| `CHARGEMAP_IMPORT_OSM_NORTH` | `...bbox.north` | `21.5` | north latitude |
| `CHARGEMAP_IMPORT_OSM_EAST` | `...bbox.east` | `127.0` | east longitude |

Bounding boxes (S, W, N, E):

- **Philippines (default):** `4.5, 116.0, 21.5, 127.0`
- **Metro Cebu (focused first launch):** `10.20, 123.75, 10.45, 124.05`
- **Metro Manila:** `14.35, 120.90, 14.75, 121.15`

### Coverage reality (verified Sep 2026)

A live probe of OSM found:

- **Metro Cebu: 0 charging stations** currently tagged in OpenStreetMap.
- **Metro Manila: ~20 stations** with real operators (Shell Recharge, AC Mobility /
  Ayala, eSakay, Unioil, Tesla, BYD, MG, Megaworld, etc.).

So OSM's PH EV data is concentrated in Metro Manila today. For a Cebu-first launch you'll
mostly rely on your community adding stations; a Manila import is what actually populates
the map right now. This is also a real opportunity: contribute Cebu stations back to OSM
(and everyone, including you, benefits on the next import).

> **Country-wide queries time out.** The public `overpass-api.de` endpoint returns a 504
> on a whole-Philippines bounding box. Import region by region (Manila, then Cebu, etc.)
> rather than one national query.

### Local run (Metro Cebu)

PowerShell:

```powershell
$env:CHARGEMAP_IMPORT_OSM = "true"
$env:CHARGEMAP_IMPORT_OSM_SOUTH = "10.20"
$env:CHARGEMAP_IMPORT_OSM_WEST  = "123.75"
$env:CHARGEMAP_IMPORT_OSM_NORTH = "10.45"
$env:CHARGEMAP_IMPORT_OSM_EAST  = "124.05"
mvn spring-boot:run
```

Watch the logs for:

```
Starting OSM charging-station import for bbox S=10.2 W=123.75 N=10.45 E=124.05
Overpass returned N charging-station elements
OSM import complete: fetched=N, created=N, updated=0, skipped=0
```

Then stop the app, unset `CHARGEMAP_IMPORT_OSM` (or set it back to `false`), and run
normally.

### On Render (or any host)

1. In the backend service **Environment**, add `CHARGEMAP_IMPORT_OSM=true` and the four
   bbox vars (use the Metro Cebu box for a focused launch).
2. Trigger a deploy / restart. The app boots, imports, then keeps serving.
3. Once the logs show the import finished, **set `CHARGEMAP_IMPORT_OSM=false`** (or delete
   the var) and redeploy so it doesn't re-run on every restart. Re-running is safe (it
   dedups), but it's an unnecessary Overpass call on each boot.

### Behind a TLS-inspecting proxy (e.g. Zscaler) — file-based import

If the backend can't make HTTPS calls to Overpass, you'll see this on import:

```
SSLHandshakeException: PKIX path building failed:
  unable to find valid certification path to requested target
```

That means a corporate proxy (Zscaler was confirmed on the dev machine here) re-signs
HTTPS with its own root CA, which the OS trusts but the **JDK truststore does not**. Two
fixes:

1. **File-based import (no cert changes — used to seed this project).** Fetch the data
   with a tool that trusts the proxy (PowerShell), then point the importer at the file:

   ```powershell
   # 1. Fetch to a file (PowerShell uses the OS trust store, so the proxy is fine).
   #    Use the kumi mirror if overpass-api.de is busy.
   powershell -ExecutionPolicy Bypass -File scripts\fetch-osm.ps1 `
     -Bbox "14.35,120.90,14.75,121.15" `
     -Out ".\data\osm-stations.json" `
     -Endpoint "https://overpass.kumi.systems/api/interpreter"

   # 2. Import from the file.
   $env:CHARGEMAP_IMPORT_OSM = "true"
   $env:CHARGEMAP_IMPORT_OSM_FILE = "data/osm-stations.json"
   mvn spring-boot:run
   ```

   `CHARGEMAP_IMPORT_OSM_FILE` overrides live fetching. Expect logs like:

   ```
   Starting OSM import from file ...\data\osm-stations.json
   Overpass returned 20 charging-station elements
   OSM import complete: fetched=20, created=20, updated=0, skipped=0
   ```

   Re-running is idempotent (a second run logs `created=0, updated=20`).

2. **Add the proxy root CA to the JDK truststore** (makes live import + all backend HTTPS
   work on that machine). Export the Zscaler root cert and
   `keytool -importcert -cacerts -alias zscaler-root -file zscaler-root.cer`. Only needed
   locally — cloud hosts like Render have no such proxy, so **live import works there
   without any of this**.

## Refreshing later

To pull new stations added to OSM since your last import, just run it again with the same
bounding box (or re-fetch the file and re-import). New OSM stations are created; existing
ones (matched by `sourceRef`) have their OSM facts refreshed while all community data is
preserved.

## Notes and limits

- **Overpass is a shared free service.** Be considerate: don't run huge country-wide
  imports repeatedly. For production refreshes, prefer a narrow bbox and infrequent runs.
- **Data quality varies.** OSM coverage in the Philippines is improving but incomplete and
  sometimes stale. That's expected — imported stations are `LOW` confidence so your users'
  edits and reports take precedence.
- **No availability or price** is imported (OSM doesn't have trustworthy real-time data).
  Those come entirely from your community layer.

## Files

| File | Purpose |
| ---- | ------- |
| `station/imports/OverpassClient.java` | Calls the Overpass API, parses elements |
| `station/imports/OsmStationMapper.java` | Maps OSM tags → `Station`/`Charger` |
| `station/imports/OsmImportService.java` | Fetch + dedup + upsert, returns a summary |
| `station/imports/OsmImportRunner.java` | Opt-in startup trigger (`@ConditionalOnProperty`) |
| `station/imports/OsmImportProperties.java` | Binds `chargemap.import.osm.*` config |
| `station/Station.java` | `sourceRef` field for dedup |
