import { createTheme } from "@mui/material/styles";

/**
 * "Kinetic Telemetry HUD" theme — a mission-critical operational cockpit for EV charging.
 * Deep void-black surfaces, piercing hyper-mint signal accents, razor-thin technical
 * borders, sharp mechanical radii, and monospaced diagnostic typography (JetBrains Mono)
 * paired with engineered geometric headlines (Space Grotesk).
 *
 * Engineered for outdoor glanceability: luminous contrast over obsidian layers, glow-based
 * elevation instead of soft shadows that wash out in sunlight.
 */

// HUD palette tokens (from design.md).
export const hud = {
  void: "#040608", // ambient ground
  surface1: "#090d14", // structural panels / station cards
  surface2: "#121824", // interactive modules / inputs / sheets
  surface3: "#1a2130", // raised/hover
  border: "#1e293b", // mechanical sub-divisions ("ghost" border)
  borderMint: "rgba(0,255,157,0.25)",
  textHigh: "#f1f5f9", // maximum-luminance labels
  textMuted: "#64748b", // engineering labels / units
  mint: "#00ff9d", // primary signal — available / active / affirmative
  mintDim: "#00e38b",
  cyan: "#00b8ff", // secondary telemetry accent
  neon: "#ff3366", // fault / destructive / critical
  amber: "#ffb800", // occupied / queued / degraded
} as const;

// Directional mint glow for floating HUD elevation.
const mintGlow = (a: number, blur = 24) => `0 0 ${blur}px rgba(0, 255, 157, ${a})`;

export const theme = createTheme({
  palette: {
    mode: "dark",
    primary: { main: hud.mint, dark: hud.mintDim, light: "#56ffa8", contrastText: hud.void },
    secondary: { main: hud.cyan, contrastText: "#001e2e" },
    success: { main: hud.mint, contrastText: hud.void },
    error: { main: hud.neon, contrastText: "#ffffff" },
    warning: { main: hud.amber, contrastText: "#040608" },
    info: { main: hud.cyan, contrastText: "#001e2e" },
    background: { default: hud.void, paper: hud.surface1 },
    text: { primary: hud.textHigh, secondary: hud.textMuted },
    divider: hud.border,
  },
  // Sharp, mechanical radii — instrument-panel feel, not consumer softness.
  shape: { borderRadius: 4 },
  typography: {
    // JetBrains Mono is the default (telemetry / body). Headlines override to Space Grotesk.
    fontFamily: "'JetBrains Mono', ui-monospace, SFMono-Regular, Menlo, monospace",
    h4: {
      fontFamily: "'Space Grotesk', system-ui, sans-serif",
      fontWeight: 700,
      letterSpacing: "-0.02em",
    },
    h5: {
      fontFamily: "'Space Grotesk', system-ui, sans-serif",
      fontWeight: 700,
      letterSpacing: "-0.02em",
    },
    h6: {
      fontFamily: "'Space Grotesk', system-ui, sans-serif",
      fontWeight: 600,
      letterSpacing: "-0.01em",
    },
    subtitle1: { fontFamily: "'Space Grotesk', system-ui, sans-serif", fontWeight: 600 },
    subtitle2: { fontFamily: "'Space Grotesk', system-ui, sans-serif", fontWeight: 600 },
    body1: { fontSize: 15, lineHeight: "22px" },
    body2: { fontSize: 13, lineHeight: "18px" },
    button: { textTransform: "uppercase", fontWeight: 600, letterSpacing: "0.06em" },
    caption: { letterSpacing: "0.06em" },
    overline: { letterSpacing: "0.12em", fontWeight: 700 },
  },
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: { backgroundColor: hud.void, color: hud.textHigh },
      },
    },
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: {
          borderRadius: 4,
          paddingTop: 10,
          paddingBottom: 10,
          paddingInline: 18,
          minHeight: 44,
        },
        contained: {
          boxShadow: mintGlow(0.18),
          "&:hover": { boxShadow: mintGlow(0.3) },
        },
        containedPrimary: {
          color: hud.void,
          fontWeight: 700,
        },
        outlined: {
          borderWidth: 1,
          borderColor: hud.mint,
          color: hud.mint,
          backgroundColor: hud.surface2,
          "&:hover": { borderWidth: 1, backgroundColor: "rgba(0,255,157,0.08)" },
        },
        text: { color: hud.textHigh },
      },
    },
    MuiCard: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: {
          borderRadius: 6,
          backgroundColor: hud.surface1,
          border: `1px solid ${hud.border}`,
          backgroundImage: "none",
          transition: "border-color .2s ease, box-shadow .2s ease",
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          fontFamily: "'JetBrains Mono', monospace",
          fontWeight: 600,
          fontSize: 11,
          letterSpacing: "0.06em",
          textTransform: "uppercase",
          borderRadius: 4,
        },
        outlined: { borderColor: hud.border },
        filledPrimary: { color: hud.void },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: { backgroundImage: "none", backgroundColor: hud.surface1 },
        rounded: { borderRadius: 8 },
        outlined: { borderColor: hud.border },
      },
    },
    MuiDrawer: {
      styleOverrides: {
        paper: {
          backgroundImage: "none",
          backgroundColor: hud.surface1,
          borderColor: hud.border,
        },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: { backgroundImage: "none", backgroundColor: hud.surface1 },
      },
    },
    MuiDialog: {
      styleOverrides: {
        paper: {
          borderRadius: 8,
          backgroundColor: hud.surface1,
          border: `1.5px solid ${hud.mint}`,
          boxShadow: mintGlow(0.25, 32),
          backgroundImage: "none",
        },
      },
    },
    MuiDivider: {
      styleOverrides: { root: { borderColor: hud.border } },
    },
    MuiBottomNavigation: {
      styleOverrides: {
        root: { borderRadius: 0, backgroundColor: "transparent" },
      },
    },
    MuiBottomNavigationAction: {
      styleOverrides: {
        root: {
          borderRadius: 4,
          margin: "6px 4px",
          color: hud.textMuted,
          "&.Mui-selected": {
            color: hud.mint,
            backgroundColor: "rgba(0,255,157,0.10)",
          },
        },
      },
    },
    MuiTextField: {
      defaultProps: { variant: "outlined" },
    },
    MuiInputBase: {
      styleOverrides: {
        input: {
          fontFamily: "'JetBrains Mono', monospace",
          fontSize: 14,
          letterSpacing: 0,
          "&::placeholder": { color: hud.textMuted, opacity: 1 },
        },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 4,
          backgroundColor: hud.surface2,
          transition: "box-shadow .15s ease, border-color .15s ease",
          "& .MuiOutlinedInput-notchedOutline": { borderColor: hud.border },
          "&:hover .MuiOutlinedInput-notchedOutline": { borderColor: hud.textMuted },
          "&.Mui-focused": {
            "& .MuiOutlinedInput-notchedOutline": {
              borderColor: hud.mint,
              borderWidth: 1.5,
            },
            boxShadow: mintGlow(0.12, 16),
          },
        },
        input: { padding: "12px 14px" },
        inputSizeSmall: { padding: "9px 12px" },
      },
    },
    MuiInputLabel: {
      styleOverrides: {
        root: {
          fontFamily: "'JetBrains Mono', monospace",
          fontSize: 13,
          color: hud.textMuted,
          "&.Mui-focused": { color: hud.mint },
        },
      },
    },
    MuiSelect: {
      styleOverrides: {
        select: { fontFamily: "'JetBrains Mono', monospace", fontSize: 14 },
      },
    },
    MuiMenuItem: {
      styleOverrides: {
        root: { fontFamily: "'JetBrains Mono', monospace", fontSize: 14 },
      },
    },
    MuiMenu: {
      styleOverrides: {
        paper: {
          borderRadius: 6,
          border: `1px solid ${hud.border}`,
          backgroundColor: hud.surface2,
          boxShadow: mintGlow(0.1, 24),
        },
      },
    },
    MuiTooltip: {
      styleOverrides: {
        tooltip: {
          backgroundColor: hud.surface3,
          border: `1px solid ${hud.border}`,
          fontFamily: "'JetBrains Mono', monospace",
          fontSize: 11,
          letterSpacing: "0.06em",
        },
      },
    },
    MuiAlert: {
      styleOverrides: {
        root: { borderRadius: 4, fontFamily: "'JetBrains Mono', monospace", fontSize: 13 },
      },
    },
    MuiTab: {
      styleOverrides: {
        root: { textTransform: "uppercase", letterSpacing: "0.06em", fontWeight: 600 },
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

/** Availability → HUD signal hex used for map markers and accents. */
export const availabilityHex = (summary: string): string => {
  switch (summary) {
    case "AVAILABLE":
      return hud.mint; // hyper-mint: operational
    case "OCCUPIED":
      return hud.amber; // amber ion: occupied/degraded
    default:
      return hud.textMuted; // unknown/telemetry-muted
  }
};

/** Availability → translucent signal fill for tonal containers. */
export const availabilityTint = (summary: string): string => {
  switch (summary) {
    case "AVAILABLE":
      return "rgba(0,255,157,0.12)";
    case "OCCUPIED":
      return "rgba(255,184,0,0.14)";
    default:
      return "rgba(100,116,139,0.14)";
  }
};

/** Status dot color for per-charger/availability status (HUD signal lights). */
export const statusDot = (status: string): string => {
  switch (status) {
    case "AVAILABLE":
      return hud.mint;
    case "OCCUPIED":
      return hud.amber;
    case "BROKEN":
      return hud.neon;
    case "CLOSED":
      return hud.textMuted;
    default:
      return hud.textMuted; // UNKNOWN
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
