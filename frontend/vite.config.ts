import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { VitePWA } from "vite-plugin-pwa";

// The frontend calls the Spring Boot API. In dev we proxy /api and /actuator to
// the backend on :8080 so there are no cross-origin surprises and no hardcoded host.
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      // Auto-update the service worker in the background; the app prompts the
      // user to reload when a new version is ready (see registerSW.ts).
      registerType: "prompt",
      // Inject our own registration so we can surface an update prompt.
      injectRegister: null,
      // Also emit an offline fallback for navigations.
      includeAssets: ["favicon.svg", "icons/*.png", "robots.txt"],
      manifest: {
        name: "ChargeMap PH",
        short_name: "ChargeMap",
        description:
          "Find, review, and add EV charging stations across Metro Cebu and the Philippines.",
        id: "/",
        start_url: "/",
        scope: "/",
        display: "standalone",
        orientation: "portrait",
        background_color: "#040608",
        theme_color: "#040608",
        lang: "en",
        categories: ["travel", "navigation", "utilities"],
        icons: [
          {
            src: "/icons/pwa-192.png",
            sizes: "192x192",
            type: "image/png",
            purpose: "any",
          },
          {
            src: "/icons/pwa-512.png",
            sizes: "512x512",
            type: "image/png",
            purpose: "any",
          },
          {
            src: "/icons/pwa-maskable-192.png",
            sizes: "192x192",
            type: "image/png",
            purpose: "maskable",
          },
          {
            src: "/icons/pwa-maskable-512.png",
            sizes: "512x512",
            type: "image/png",
            purpose: "maskable",
          },
        ],
      },
      workbox: {
        // Precache the built app shell (JS/CSS/HTML/fonts/images).
        globPatterns: ["**/*.{js,css,html,svg,png,ico,woff,woff2}"],
        // Some bundles (Tesseract OCR, Leaflet) are large; raise the limit.
        maximumFileSizeToCacheInBytes: 6 * 1024 * 1024,
        // SPA navigation fallback so deep links work offline.
        navigateFallback: "/index.html",
        // Never let the SW intercept API / actuator calls.
        navigateFallbackDenylist: [/^\/api\//, /^\/actuator\//],
        runtimeCaching: [
          {
            // API data: always try the network first, fall back to cache when offline.
            urlPattern: ({ url }) => url.pathname.startsWith("/api/"),
            handler: "NetworkFirst",
            options: {
              cacheName: "chargemap-api",
              networkTimeoutSeconds: 5,
              expiration: { maxEntries: 200, maxAgeSeconds: 60 * 60 * 24 },
              cacheableResponse: { statuses: [0, 200] },
            },
          },
          {
            // Map tiles (Leaflet / OSM / Mapbox): cache-first, they're immutable.
            urlPattern: ({ url }) =>
              /tile|tiles|mapbox|openstreetmap|basemaps/i.test(url.href),
            handler: "CacheFirst",
            options: {
              cacheName: "chargemap-map-tiles",
              expiration: { maxEntries: 500, maxAgeSeconds: 60 * 60 * 24 * 30 },
              cacheableResponse: { statuses: [0, 200] },
            },
          },
          {
            // Uploaded images served from the API/static host.
            urlPattern: ({ request }) => request.destination === "image",
            handler: "StaleWhileRevalidate",
            options: {
              cacheName: "chargemap-images",
              expiration: { maxEntries: 300, maxAgeSeconds: 60 * 60 * 24 * 14 },
              cacheableResponse: { statuses: [0, 200] },
            },
          },
        ],
      },
      devOptions: {
        // Keep the SW off in `vite dev` to avoid caching surprises while coding.
        enabled: false,
      },
    }),
  ],
  server: {
    port: 5173,
    proxy: {
      "/api": { target: "http://localhost:8080", changeOrigin: true },
      "/actuator": { target: "http://localhost:8080", changeOrigin: true },
    },
  },
});
