# Publishing ChargeMap to Google Play (PWA → TWA)

ChargeMap's frontend is now an installable Progressive Web App (PWA). To ship it on
the **Google Play Store**, you wrap that PWA in a **Trusted Web Activity (TWA)** — a
thin Android shell that opens your live site full-screen, with no browser UI. The tool
that generates the Android project + signed bundle is **Bubblewrap**.

There are two ways to install the app on Android:

1. **Directly from the browser** (no store) — visit the site in Chrome and use
   "Install app" / "Add to Home screen". This already works with the PWA changes.
2. **Through Google Play** — the TWA route documented below.

---

## 1. Prerequisites

- The PWA must be deployed on **HTTPS** at a stable public URL. Current frontend:
  `https://chargemap-web.onrender.com` (replace with your custom domain if you add one).
- A **Google Play Developer account** (one-time US$25 registration fee).
- **Node.js 18+** and **Java JDK 17+** installed locally.
- **Android SDK** — Bubblewrap can download and manage it for you on first run.

### Verify the PWA is installable first

Open the deployed site in Chrome desktop → DevTools → **Application** tab → **Manifest**
and **Service Workers**. You should see:

- Manifest parsed with name, icons (192 + 512, including a maskable icon), `start_url`,
  `display: standalone`, `theme_color`.
- A registered, activated service worker.
- Lighthouse → **PWA** category should report the app as installable.

If Lighthouse flags "installable", fix that before packaging — the TWA build depends on it.

---

## 2. Install Bubblewrap

```bash
npm install -g @bubblewrap/cli
```

First run will offer to download the JDK + Android SDK if they aren't found. Accept, or
point it at existing installs.

---

## 3. Initialize the TWA project

Run this in a **new empty folder** (not inside the web repo), pointing at the deployed
manifest:

```bash
bubblewrap init --manifest https://chargemap-web.onrender.com/manifest.webmanifest
```

Bubblewrap reads the manifest and prompts for values. Recommended answers:

| Prompt                         | Value                                             |
| ------------------------------ | ------------------------------------------------- |
| Application name               | ChargeMap PH                                       |
| Short name                     | ChargeMap                                          |
| Application ID (package)       | `ph.chargemap.twa`  *(must match assetlinks.json)* |
| Launcher name                  | ChargeMap                                          |
| Display mode                   | standalone                                         |
| Orientation                    | portrait                                           |
| Theme color                    | `#1f9d57`                                          |
| Background color               | `#f3f6f1`                                          |
| Start URL                      | `/`                                                |
| Icon URL                       | `https://chargemap-web.onrender.com/icons/pwa-512.png` |
| Maskable icon URL              | `https://chargemap-web.onrender.com/icons/pwa-maskable-512.png` |
| Include support for Play Billing | No                                              |
| Signing key                    | Create new (see next step)                         |

> **Important:** the **Application ID** you choose here must exactly equal the
> `package_name` in `frontend/public/.well-known/assetlinks.json`
> (currently `ph.chargemap.twa`). If you change one, change the other.

---

## 4. Create / manage the signing key

Bubblewrap can generate a keystore during `init`, or create one manually:

```bash
keytool -genkeypair -v \
  -keystore chargemap-release.keystore \
  -alias chargemap \
  -keyalg RSA -keysize 2048 -validity 10000
```

**Back this keystore + password up somewhere safe.** Losing it means you can never ship
an update to the same Play listing.

> If you enable **Google Play App Signing** (recommended, and default for new apps),
> Google holds the final signing key and you upload with an *upload key*. In that case the
> SHA-256 that goes into `assetlinks.json` is the **App signing key** fingerprint shown in
> Play Console, not your local keystore's. See step 7.

---

## 5. Build the Android bundle

```bash
bubblewrap build
```

Outputs:

- `app-release-bundle.aab` — upload this to Play.
- `app-release-signed.apk` — for local sideload testing.

Install the APK on a device to smoke-test:

```bash
adb install app-release-signed.apk
```

The app should launch full-screen with no browser chrome. If you see a URL bar at the
top, Digital Asset Links verification failed — fix step 6/7 before publishing.

---

## 6. Digital Asset Links (proves you own the domain)

A placeholder already exists at `frontend/public/.well-known/assetlinks.json` and is
served at:

```
https://chargemap-web.onrender.com/.well-known/assetlinks.json
```

You must replace `REPLACE_WITH_YOUR_APP_SIGNING_SHA256_FINGERPRINT` with the real
SHA-256 fingerprint (see step 7), redeploy the frontend, and confirm the file is publicly
reachable and served as JSON.

`bubblewrap fingerprint` can print the local keystore fingerprint, and Bubblewrap can even
generate the assetlinks content:

```bash
bubblewrap fingerprint generateAssetLinks
```

The final file must look like:

```json
[
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "ph.chargemap.twa",
      "sha256_cert_fingerprints": ["AA:BB:CC:...:99"]
    }
  }
]
```

Verify with Google's tester:
`https://developers.google.com/digital-asset-links/tools/generator`

---

## 7. Which fingerprint goes in assetlinks.json?

- **Using Google Play App Signing (recommended):** after you upload the first bundle,
  open **Play Console → your app → Setup → App integrity → App signing**. Copy the
  **SHA-256 certificate fingerprint** under "App signing key certificate". That is the
  value that belongs in `assetlinks.json`. (Optionally also add the **upload key**
  fingerprint so sideloaded upload-signed builds verify during testing.)
- **Self-signing only:** use your release keystore's SHA-256 from
  `bubblewrap fingerprint` / `keytool -list -v -keystore ...`.

After updating `assetlinks.json`, **redeploy the frontend** and re-verify. Asset Links are
cached, so allow time / force re-check on device.

---

## 8. Upload to Play Console

1. **Play Console → Create app.** Set name (ChargeMap PH), default language, "App", free.
2. Complete the required declarations: privacy policy URL, data safety form, content
   rating questionnaire, target audience, ads declaration.
3. **Production → Create new release.** Upload `app-release-bundle.aab`.
4. Enable **Google Play App Signing** when prompted (default). Grab the App signing SHA-256
   and finish step 6/7 if you hadn't already.
5. Add store listing assets: short + full description, 512×512 app icon, feature graphic
   (1024×500), and at least 2 phone screenshots.
6. Roll out to **Internal testing** first to confirm the TWA opens full-screen and Asset
   Links verify, then promote to Production.

---

## 9. Shipping updates

Two independent update paths:

- **Web content / features:** just deploy the frontend. The TWA loads your live site, so
  users get web updates immediately — **no Play submission needed** for content changes.
  The in-app "New version available → Reload" prompt (service worker) handles cache refresh.
- **Native shell changes** (app name, icon, target SDK bump, TWA config): bump
  `versionCode`/`versionName` in the Bubblewrap project, `bubblewrap build`, and upload a
  new `.aab` to Play.

---

## 10. Files in this repo that support this

| File | Purpose |
| ---- | ------- |
| `frontend/vite.config.ts` | `VitePWA` config: manifest + Workbox service worker + runtime caching |
| `frontend/index.html` | PWA meta tags, manifest link, icon links |
| `frontend/src/pwa/PwaUpdater.tsx` | Registers the SW, shows update / offline-ready prompts |
| `frontend/public/manifest*` | Generated at build → `dist/manifest.webmanifest` |
| `frontend/public/icons/*` | App icons (192/512 `any` + `maskable`, apple-touch) |
| `frontend/public/.well-known/assetlinks.json` | Digital Asset Links for the TWA (edit fingerprint) |
| `frontend/scripts/gen-icons.mjs` | Regenerates PNG icons from the brand SVGs |

### Regenerating icons

If you change the logo (`frontend/public/favicon.svg` or
`frontend/public/icons/maskable.svg`):

```bash
cd frontend
node scripts/gen-icons.mjs
npm run build
```

---

## Notes for the Render Static Site deployment

Render serves the built `dist/` directory directly, so `manifest.webmanifest`, `sw.js`,
the icons, and `.well-known/assetlinks.json` are all served automatically at the correct
paths. Two things to confirm in the Render dashboard:

- The `/api/*` and `/actuator/*` **rewrite rules** stay **above** the `/*` → `/index.html`
  SPA fallback (already required for the app to work). The service worker explicitly
  ignores `/api/*` and `/actuator/*`, so it won't interfere.
- No aggressive CDN caching on `sw.js` or `manifest.webmanifest`. If you add custom
  headers, mark those `Cache-Control: no-cache` so clients pick up new releases.

The `frontend/nginx.conf` rules (no-cache for `sw.js`/manifest, JSON type for
`assetlinks.json`) only apply if you deploy the **Docker/nginx** image instead of the
Render Static Site.
