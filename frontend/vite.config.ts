import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// The frontend calls the Spring Boot API. In dev we proxy /api and /actuator to
// the backend on :8080 so there are no cross-origin surprises and no hardcoded host.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": { target: "http://localhost:8080", changeOrigin: true },
      "/actuator": { target: "http://localhost:8080", changeOrigin: true },
    },
  },
});
