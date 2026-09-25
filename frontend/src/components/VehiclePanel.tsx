import { useCallback, useEffect, useRef, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Divider,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { Checkbox, FormControlLabel } from "@mui/material";
import DirectionsCarFilledRoundedIcon from "@mui/icons-material/DirectionsCarFilledRounded";
import BoltRoundedIcon from "@mui/icons-material/BoltRounded";
import PlaceRoundedIcon from "@mui/icons-material/PlaceRounded";
import SyncRoundedIcon from "@mui/icons-material/SyncRounded";
import { api } from "../api/client";
import type {
  BatteryTier,
  ConnectedVehicle,
  VehicleLocation,
  VehicleManufacturer,
} from "../api/types";

const MANUFACTURERS: { value: VehicleManufacturer; label: string }[] = [
  { value: "TESLA", label: "Tesla" },
  { value: "FORD", label: "Ford" },
  { value: "BMW", label: "BMW" },
  { value: "HYUNDAI", label: "Hyundai" },
  { value: "KIA", label: "Kia" },
  { value: "MERCEDES", label: "Mercedes-Benz" },
];

const TIER_COLOR: Record<BatteryTier, string> = {
  EXCELLENT: "#1f9d57",
  GOOD: "#4caf50",
  LOW: "#f59e0b",
  CRITICAL: "#dc2626",
  UNKNOWN: "#9ca3af",
};

function timeAgo(iso: string | null): string {
  if (!iso) return "never";
  const secs = Math.round((Date.now() - new Date(iso).getTime()) / 1000);
  if (secs < 60) return "just now";
  const mins = Math.round(secs / 60);
  if (mins < 60) return `${mins} min ago`;
  const h = Math.round(mins / 60);
  return `${h} h ago`;
}

/** Circular battery gauge colored by tier. */
function BatteryRing({ percent, tier }: { percent: number | null; tier: BatteryTier }) {
  const size = 96;
  const stroke = 9;
  const r = (size - stroke) / 2;
  const circ = 2 * Math.PI * r;
  const pct = percent ?? 0;
  const dash = (pct / 100) * circ;
  const color = TIER_COLOR[tier];
  return (
    <Box sx={{ position: "relative", width: size, height: size, flexShrink: 0 }}>
      <svg width={size} height={size}>
        <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke="#eceff1" strokeWidth={stroke} />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={r}
          fill="none"
          stroke={color}
          strokeWidth={stroke}
          strokeDasharray={`${dash} ${circ}`}
          strokeLinecap="round"
          transform={`rotate(-90 ${size / 2} ${size / 2})`}
        />
      </svg>
      <Box
        sx={{
          position: "absolute",
          inset: 0,
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        <Typography sx={{ fontSize: 22, fontWeight: 800, lineHeight: 1, color }}>
          {percent != null ? `${percent}%` : "—"}
        </Typography>
        <Typography sx={{ fontSize: 10, color: "text.secondary" }}>battery</Typography>
      </Box>
    </Box>
  );
}

interface Props {
  /** Compact variant hides the connect form's extra chrome (used inside dialogs). */
  onConnected?: (v: ConnectedVehicle) => void;
}

/**
 * Opt-in vehicle sync panel for the profile: shows the connect form when the user has no
 * vehicle, otherwise the live telemetry dashboard. Polls status + location while mounted.
 */
export default function VehiclePanel({ onConnected }: Props) {
  const [vehicles, setVehicles] = useState<ConnectedVehicle[] | null>(null);
  const [location, setLocation] = useState<VehicleLocation | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Connect form
  const [manufacturer, setManufacturer] = useState<VehicleManufacturer>("TESLA");
  const [nickname, setNickname] = useState("");
  const [locationConsent, setLocationConsent] = useState(true);

  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const load = useCallback(async () => {
    try {
      const list = await api.listVehicles();
      setVehicles(list);
      if (list.length > 0) {
        setLocation(await api.vehicleLocation(list[0].id));
      }
    } catch {
      setError("Could not load your vehicles.");
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  // Poll live status/location every 30s while a vehicle is connected.
  useEffect(() => {
    if (!vehicles || vehicles.length === 0) return;
    pollRef.current = setInterval(async () => {
      try {
        const status = await api.vehicleStatus(vehicles[0].id);
        setVehicles((prev) => (prev ? prev.map((v) => (v.id === status.id ? status : v)) : prev));
        setLocation(await api.vehicleLocation(vehicles[0].id));
      } catch {
        /* transient; ignore */
      }
    }, 30000);
    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
    };
  }, [vehicles]);

  const connect = async () => {
    setBusy(true);
    setError(null);
    try {
      const v = await api.connectVehicle({
        manufacturer,
        nickname: nickname.trim() || undefined,
        locationConsent,
      });
      setNickname("");
      onConnected?.(v);
      await load();
    } catch {
      setError("Could not connect the vehicle. Please try again.");
    } finally {
      setBusy(false);
    }
  };

  const syncNow = async () => {
    setBusy(true);
    try {
      await api.syncVehicles();
      await load();
    } finally {
      setBusy(false);
    }
  };

  const disconnect = async (id: string) => {
    setBusy(true);
    try {
      await api.disconnectVehicle(id);
      setLocation(null);
      await load();
    } finally {
      setBusy(false);
    }
  };

  if (vehicles === null) {
    return (
      <Stack alignItems="center" py={3}>
        <CircularProgress size={24} />
      </Stack>
    );
  }

  // ----- Connect form (no vehicle yet) -----
  if (vehicles.length === 0) {
    return (
      <Box>
        <Stack direction="row" spacing={1} alignItems="center" mb={1}>
          <DirectionsCarFilledRoundedIcon color="primary" />
          <Typography sx={{ fontSize: 14, fontWeight: 700 }}>Connect your EV</Typography>
        </Stack>
        <Typography variant="body2" color="text.secondary" mb={1.5}>
          Sync your vehicle to see live battery, range, charging, and location. Optional —
          you can do this anytime.
        </Typography>
        {error && <Alert severity="error" sx={{ mb: 1.5 }}>{error}</Alert>}
        <Stack spacing={1.5}>
          <TextField
            select
            size="small"
            label="Manufacturer"
            value={manufacturer}
            onChange={(e) => setManufacturer(e.target.value as VehicleManufacturer)}
            fullWidth
          >
            {MANUFACTURERS.map((m) => (
              <MenuItem key={m.value} value={m.value}>
                {m.label}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            size="small"
            label="Nickname (optional)"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            placeholder="e.g. My Model 3"
            fullWidth
          />
          <FormControlLabel
            control={
              <Checkbox
                size="small"
                checked={locationConsent}
                onChange={(e) => setLocationConsent(e.target.checked)}
              />
            }
            label={
              <Typography variant="body2" color="text.secondary">
                Allow location tracking for this vehicle
              </Typography>
            }
          />
          <Button
            variant="contained"
            startIcon={<DirectionsCarFilledRoundedIcon />}
            onClick={connect}
            disabled={busy}
          >
            {busy ? "Connecting…" : "Connect vehicle"}
          </Button>
          <Typography variant="caption" color="text.secondary">
            You'll be redirected to your manufacturer to authorize access. Tokens are stored
            encrypted.
          </Typography>
        </Stack>
      </Box>
    );
  }

  // ----- Dashboard (connected) -----
  const v = vehicles[0];
  const t = v.telemetry;
  return (
    <Box>
      {error && <Alert severity="error" sx={{ mb: 1.5 }}>{error}</Alert>}
      <Stack direction="row" spacing={2} alignItems="center">
        <BatteryRing percent={t?.batteryPercentage ?? null} tier={t?.batteryTier ?? "UNKNOWN"} />
        <Box sx={{ minWidth: 0, flex: 1 }}>
          <Typography sx={{ fontSize: 15, fontWeight: 700 }} noWrap>
            {v.nickname || v.model || "My EV"}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {v.manufacturer} {v.year ? `· ${v.year}` : ""}
          </Typography>
          <Stack direction="row" spacing={0.75} alignItems="center" mt={0.75} flexWrap="wrap" useFlexGap>
            {t?.rangeKm != null && (
              <Chip size="small" label={`${Math.round(t.rangeKm)} km range`} variant="outlined" />
            )}
            {t?.chargingStatus && (
              <Chip
                size="small"
                icon={<BoltRoundedIcon />}
                label={t.chargingStatus === "CHARGING"
                  ? `Charging${t.chargingSpeedKw ? ` · ${Math.round(t.chargingSpeedKw)} kW` : ""}`
                  : t.chargingStatus.charAt(0) + t.chargingStatus.slice(1).toLowerCase()}
                color={t.chargingStatus === "CHARGING" ? "success" : "default"}
              />
            )}
          </Stack>
        </Box>
      </Stack>

      <Stack direction="row" spacing={2} mt={1.5}>
        <Box>
          <Typography variant="caption" color="text.secondary">
            Battery health
          </Typography>
          <Typography sx={{ fontSize: 15, fontWeight: 600 }}>
            {t?.batteryHealthPercent != null ? `${t.batteryHealthPercent}%` : "—"}
          </Typography>
        </Box>
        <Box>
          <Typography variant="caption" color="text.secondary">
            Odometer
          </Typography>
          <Typography sx={{ fontSize: 15, fontWeight: 600 }}>
            {t?.odometerKm != null ? `${Math.round(t.odometerKm).toLocaleString()} km` : "—"}
          </Typography>
        </Box>
      </Stack>

      {v.locationConsent && location?.latitude != null && location?.longitude != null && (
        <Stack direction="row" spacing={0.5} alignItems="center" mt={1.25}>
          <PlaceRoundedIcon sx={{ fontSize: 16, color: "text.secondary" }} />
          <Typography variant="caption" color="text.secondary">
            {location.latitude.toFixed(4)}, {location.longitude.toFixed(4)}
          </Typography>
        </Stack>
      )}

      <Divider sx={{ my: 1.5 }} />
      <Stack direction="row" spacing={1} alignItems="center" justifyContent="space-between">
        <Typography variant="caption" color="text.secondary">
          Synced {timeAgo(v.lastSyncAt)}
        </Typography>
        <Stack direction="row" spacing={1}>
          <Button
            size="small"
            startIcon={<SyncRoundedIcon />}
            onClick={syncNow}
            disabled={busy}
          >
            Sync now
          </Button>
          <Button size="small" color="error" onClick={() => disconnect(v.id)} disabled={busy}>
            Disconnect
          </Button>
        </Stack>
      </Stack>
    </Box>
  );
}
