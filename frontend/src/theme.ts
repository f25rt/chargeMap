import { createTheme } from "@mui/material/styles";

/**
 * Material 3 (Material You) inspired theme: soft rounded surfaces, tonal containers,
 * gentle layered shadows, generous spacing, and a warm green "energy" palette.
 * Aims for a "creamy", tactile mobile feel rather than flat outlined boxes.
 */

// Soft, layered elevation shadows (softer + wider than MUI defaults).
const softShadow = (y: number, blur: number, a: number) =>
  `0 ${y}px ${blur}px rgba(20, 45, 30, ${a})`;

export const theme = createTheme({
  palette: {
    mode: "light",
    primary: { main: "#1f9d57", dark: "#16773f", light: "#5cc184", contrastText: "#fff" },
    secondary: { main: "#f2a900", contrastText: "#3a2d00" },
    success: { main: "#1f9d57" },
    error: { main: "#e5484d" },
    warning: { main: "#f2a900" },
    // Warm, slightly green-tinted "cream" background instead of cold grey.
    background: { default: "#f3f6f1", paper: "#ffffff" },
    text: { primary: "#161d18", secondary: "#5d6b62" },
    divider: "rgba(30, 60, 40, 0.08)",
  },
  shape: { borderRadius: 16 },
  typography: {
    fontFamily:
      "Inter, -apple-system, BlinkMacSystemFont, 'SF Pro Text', system-ui, Arial, sans-serif",
    h5: {
      fontFamily: "'Plus Jakarta Sans', Inter, system-ui, sans-serif",
      fontWeight: 800,
      letterSpacing: -0.5,
    },
    h6: {
      fontFamily: "'Plus Jakarta Sans', Inter, system-ui, sans-serif",
      fontWeight: 700,
      letterSpacing: -0.4,
    },
    subtitle1: { fontWeight: 700, letterSpacing: -0.2 },
    body2: { letterSpacing: -0.1 },
    button: { textTransform: "none", fontWeight: 600, letterSpacing: 0 },
    caption: { letterSpacing: 0 },
  },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: { borderRadius: 999, paddingTop: 10, paddingBottom: 10, paddingInline: 20 },
        contained: {
          boxShadow: "0 6px 16px rgba(31,157,87,.28)",
          "&:hover": { boxShadow: "0 8px 22px rgba(31,157,87,.34)" },
        },
        outlined: { borderWidth: 1.5, "&:hover": { borderWidth: 1.5 } },
      },
    },
    MuiCard: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: {
          borderRadius: 16,
          boxShadow: softShadow(2, 10, 0.05),
          transition: "box-shadow .2s ease, transform .2s ease",
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: { fontWeight: 600, borderRadius: 999 },
        outlined: { borderColor: "rgba(30,60,40,0.16)" },
      },
    },
    MuiPaper: {
      styleOverrides: {
        rounded: { borderRadius: 24 },
      },
    },
    MuiDrawer: {
      styleOverrides: {
        paper: { backgroundImage: "none" },
      },
    },
    MuiBottomNavigation: {
      styleOverrides: {
        root: { borderRadius: 0 },
      },
    },
    MuiBottomNavigationAction: {
      styleOverrides: {
        root: {
          borderRadius: 16,
          margin: "6px 4px",
          "&.Mui-selected": { backgroundColor: "rgba(31,157,87,.12)" },
        },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: { borderRadius: 16 },
      },
    },
  },
});

/** Availability → MUI color mapping used across the UI. */
export const availabilityColor = (summary: string): "success" | "error" | "warning" => {
  switch (summary) {
    case "AVAILABLE":
      return "success";
    case "OCCUPIED":
      return "error";
    default:
      return "warning";
  }
};

/** Availability → hex used for map markers and accents. */
export const availabilityHex = (summary: string): string => {
  switch (summary) {
    case "AVAILABLE":
      return "#1f9d57";
    case "OCCUPIED":
      return "#e5484d";
    default:
      return "#f2a900";
  }
};

/** Availability → very soft tonal container background (Material 3 style). */
export const availabilityTint = (summary: string): string => {
  switch (summary) {
    case "AVAILABLE":
      return "rgba(31,157,87,.10)";
    case "OCCUPIED":
      return "rgba(229,72,77,.10)";
    default:
      return "rgba(242,169,0,.12)";
  }
};

/** Status dot color for charger/availability status (clean accent, not a fill). */
export const statusDot = (status: string): string => {
  switch (status) {
    case "AVAILABLE":
      return "#16a34a";
    case "OCCUPIED":
      return "#dc2626";
    case "BROKEN":
      return "#dc2626";
    case "CLOSED":
      return "#6b7280";
    default:
      return "#f59e0b"; // UNKNOWN
  }
};

/** Human label for a status value. */
export const statusLabel = (status: string): string => {
  switch (status) {
    case "AVAILABLE":
      return "Available";
    case "OCCUPIED":
      return "Occupied";
    case "BROKEN":
      return "Broken";
    case "CLOSED":
      return "Closed";
    default:
      return "Unknown";
  }
};
