import React from "react";
import ReactDOM from "react-dom/client";
import { CssBaseline, ThemeProvider } from "@mui/material";
// Kinetic Telemetry HUD typefaces: Space Grotesk (headlines/metrics) + JetBrains Mono
// (body/telemetry/labels).
import "@fontsource/space-grotesk/500.css";
import "@fontsource/space-grotesk/600.css";
import "@fontsource/space-grotesk/700.css";
import "@fontsource/jetbrains-mono/400.css";
import "@fontsource/jetbrains-mono/500.css";
import "@fontsource/jetbrains-mono/600.css";
import "@fontsource/jetbrains-mono/700.css";
import "leaflet/dist/leaflet.css";
import "./index.css";
import { theme } from "./theme";
import { AuthProvider } from "./auth/AuthContext";
import App from "./App";
import PwaUpdater from "./pwa/PwaUpdater";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <AuthProvider>
        <App />
      </AuthProvider>
      <PwaUpdater />
    </ThemeProvider>
  </React.StrictMode>,
);
