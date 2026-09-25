import { useEffect, useState } from "react";
import {
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  Divider,
  InputAdornment,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import EvStationRoundedIcon from "@mui/icons-material/EvStationRounded";
import InboxRoundedIcon from "@mui/icons-material/InboxRounded";
import PeopleRoundedIcon from "@mui/icons-material/PeopleRounded";
import EmojiEventsRoundedIcon from "@mui/icons-material/EmojiEventsRounded";
import BoltRoundedIcon from "@mui/icons-material/BoltRounded";
import UsersPanel from "./panels/UsersPanel";
import SubmissionsPanel from "./panels/SubmissionsPanel";
import RewardsPanel from "./panels/RewardsPanel";
import { api } from "../api/client";
import type { ChargerStatus, StationDetail } from "../api/types";
import { statusDot, statusLabel } from "../theme";
import DashboardShell from "./DashboardShell";

const STATUSES: ChargerStatus[] = ["AVAILABLE", "OCCUPIED", "BROKEN", "CLOSED", "UNKNOWN"];

function StationsPanel() {
  const [stations, setStations] = useState<StationDetail[]>([]);
  const [loading, setLoading] = useState(true);
  const [savingId, setSavingId] = useState<string | null>(null);
  const [priceDraft, setPriceDraft] = useState<Record<string, string>>({});

  const load = async () => {
    setLoading(true);
    try {
      setStations(await api.operatorStations());
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const patch = (updated: StationDetail) =>
    setStations((prev) => prev.map((s) => (s.id === updated.id ? updated : s)));

  const setChargerStatus = async (stationId: string, chargerId: string, status: ChargerStatus) => {
    setSavingId(chargerId);
    try {
      patch(await api.operatorSetChargerStatus(stationId, chargerId, status));
    } finally {
      setSavingId(null);
    }
  };

  const savePrice = async (stationId: string) => {
    const raw = priceDraft[stationId];
    if (raw == null || raw === "") return;
    setSavingId(stationId);
    try {
      patch(await api.operatorUpdatePricing(stationId, Number(raw)));
      setPriceDraft((d) => ({ ...d, [stationId]: "" }));
    } finally {
      setSavingId(null);
    }
  };

  if (loading) {
    return (
      <Stack alignItems="center" py={6}>
        <CircularProgress />
      </Stack>
    );
  }

  if (stations.length === 0) {
    return (
      <Stack alignItems="center" py={6} color="text.secondary">
        <BoltRoundedIcon sx={{ fontSize: 40, opacity: 0.4 }} />
        <Typography>No stations to manage yet.</Typography>
      </Stack>
    );
  }

  return (
    <Box
      sx={{
        display: "grid",
        gridTemplateColumns: { xs: "1fr", md: "1fr 1fr", xl: "1fr 1fr 1fr" },
        gap: 2,
      }}
    >
      {stations.map((s) => (
        <Card key={s.id} sx={{ p: 2 }}>
          <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
            <Box sx={{ minWidth: 0 }}>
              <Typography variant="subtitle1" noWrap>
                {s.name}
              </Typography>
              <Typography variant="body2" color="text.secondary" noWrap>
                {s.operator ?? "—"} · {s.availableCount}/{s.totalChargers} available
              </Typography>
            </Box>
            {s.disabled && <Chip label="Disabled" size="small" color="error" />}
          </Stack>

          <Divider sx={{ my: 1.5 }} />

          <Stack spacing={1.25}>
            {s.chargers.map((c, i) => (
              <Stack
                key={c.chargerId}
                direction="row"
                alignItems="center"
                justifyContent="space-between"
                spacing={1}
              >
                <Box sx={{ minWidth: 0 }}>
                  <Typography variant="body2" fontWeight={600} noWrap>
                    #{i + 1} · {c.connectorType} · {c.powerKw} kW
                  </Typography>
                  <Stack direction="row" spacing={0.5} alignItems="center">
                    <Box
                      sx={{ width: 7, height: 7, borderRadius: "50%", bgcolor: statusDot(c.status) }}
                    />
                    <Typography variant="caption" sx={{ color: statusDot(c.status) }}>
                      {statusLabel(c.status)}
                    </Typography>
                  </Stack>
                </Box>
                <TextField
                  select
                  size="small"
                  SelectProps={{ native: true }}
                  value={c.status}
                  disabled={savingId === c.chargerId}
                  onChange={(e) =>
                    setChargerStatus(s.id, c.chargerId, e.target.value as ChargerStatus)
                  }
                  sx={{ width: 140 }}
                >
                  {STATUSES.map((st) => (
                    <option key={st} value={st}>
                      {statusLabel(st)}
                    </option>
                  ))}
                </TextField>
              </Stack>
            ))}
          </Stack>

          <Divider sx={{ my: 1.5 }} />

          <Stack direction="row" spacing={1} alignItems="center">
            <Typography variant="body2" sx={{ flex: 1 }}>
              Current price:{" "}
              <strong>{s.currentPricing ? `₱${s.currentPricing.pricePerKwh}/kWh` : "—"}</strong>
            </Typography>
            <TextField
              size="small"
              type="number"
              placeholder="New ₱/kWh"
              value={priceDraft[s.id] ?? ""}
              onChange={(e) => setPriceDraft((d) => ({ ...d, [s.id]: e.target.value }))}
              InputProps={{ startAdornment: <InputAdornment position="start">₱</InputAdornment> }}
              sx={{ width: 130 }}
            />
            <Button
              variant="contained"
              disabled={savingId === s.id || !priceDraft[s.id]}
              onClick={() => savePrice(s.id)}
            >
              Save
            </Button>
          </Stack>
        </Card>
      ))}
    </Box>
  );
}

export default function OperatorApp() {
  return (
    <DashboardShell
      title="ChargeMap Operator"
      roleLabel="OPERATOR"
      items={[
        { label: "Stations", icon: <EvStationRoundedIcon />, render: () => <StationsPanel /> },
        { label: "Submissions", icon: <InboxRoundedIcon />, render: () => <SubmissionsPanel /> },
        { label: "Users", icon: <PeopleRoundedIcon />, render: () => <UsersPanel /> },
        { label: "Rewards", icon: <EmojiEventsRoundedIcon />, render: () => <RewardsPanel /> },
      ]}
    />
  );
}
