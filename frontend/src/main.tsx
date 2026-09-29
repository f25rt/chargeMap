import React from "react";
import ReactDOM from "react-dom/client";
import { CssBaseline, ThemeProvider } from "@mui/material";
import "@fontsource/inter/400.css";
import "@fontsource/inter/500.css";
import "@fontsource/inter/600.css";
import "@fontsource/inter/700.css";
import "@fontsource/plus-jakarta-sans/600.css";
import "@fontsource/plus-jakarta-sans/700.css";
import "@fontsource/plus-jakarta-sans/800.css";
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
