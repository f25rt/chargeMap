import { useEffect, useRef, useState } from "react";
import {
  Alert,
  Avatar,
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  Divider,
  LinearProgress,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import AddRoundedIcon from "@mui/icons-material/AddRounded";
import ApartmentRoundedIcon from "@mui/icons-material/ApartmentRounded";
import PhotoCameraRoundedIcon from "@mui/icons-material/PhotoCameraRounded";
import TrendingUpRoundedIcon from "@mui/icons-material/TrendingUpRounded";
import { api } from "../../api/client";
import type { AdminSummary, Branch, BranchPriceTrend, StationDetail } from "../../api/types";

export default function BranchesPanel() {
  const [branches, setBranches] = useState<Branch[]>([]);
  const [admins, setAdmins] = useState<AdminSummary[]>([]);
  const [stations, setStations] = useState<StationDetail[]>([]);
  const [trend, setTrend] = useState<BranchPriceTrend[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // New branch form
  const [name, setName] = useState("");
  const [desc, setDesc] = useState("");
  const [area, setArea] = useState("");
  const [saving, setSaving] = useState(false);

  // Station update-frequency lookup
  const [freqStationId, setFreqStationId] = useState("");
  const [freq, setFreq] = useState<string | null>(null);
  const [trendDays, setTrendDays] = useState(7);
  const imgRefs = useRef<Record<string, HTMLInputElement | null>>({});

  const load = async () => {
    setLoading(true);
    try {
      const [b, a, s, t] = await Promise.all([
        api.branches(),
        api.admins(),
        api.adminStations(),
        api.branchPriceTrend(trendDays),
      ]);
      setBranches(b);
      setAdmins(a);
      setStations(s);
      setTrend(t);
    } catch {
      setError("Failed to load branch data.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    api.branchPriceTrend(trendDays).then(setTrend).catch(() => {});
  }, [trendDays]);

  const createBranch = async () => {
    if (!name.trim()) return;
    setSaving(true);
    try {
      await api.createBranch({ name: name.trim(), description: desc.trim(), area: area.trim() });
      setName("");
      setDesc("");
      setArea("");
      await load();
    } finally {
      setSaving(false);
    }
  };

  const uploadImage = async (branchId: string, kind: "profile" | "banner", file: File | null) => {
    if (!file) return;
    await api.setBranchImage(branchId, kind, file);
    await load();
  };

  const lookupFrequency = async () => {
    if (!freqStationId) return;
    const f = await api.stationUpdateFrequency(freqStationId, 30);
    setFreq(
      `${f.stationName}: ${f.approvedUpdates} approved update(s), ${f.priceChanges} price change(s) in last ${f.windowDays} days.`,
    );
  };

  if (loading) {
    return (
      <Stack alignItems="center" py={6}>
        <CircularProgress />
      </Stack>
    );
  }

  const maxTrend = trend.reduce((m, t) => Math.max(m, t.priceChanges), 0) || 1;

  return (
    <Stack spacing={3}>
      {error && <Alert severity="error">{error}</Alert>}

      {/* Create branch */}
      <Card sx={{ p: 2 }}>
        <Stack direction="row" spacing={1} alignItems="center" mb={1}>
          <ApartmentRoundedIcon color="primary" />
          <Typography variant="subtitle1">Create branch</Typography>
        </Stack>
        <Divider sx={{ mb: 2 }} />
        <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5} alignItems="flex-start">
          <TextField size="small" label="Name" value={name} onChange={(e) => setName(e.target.value)} fullWidth />
          <TextField size="small" label="Area" value={area} onChange={(e) => setArea(e.target.value)} fullWidth />
          <TextField size="small" label="Description" value={desc} onChange={(e) => setDesc(e.target.value)} fullWidth />
          <Button
            variant="contained"
            startIcon={<AddRoundedIcon />}
            onClick={createBranch}
            disabled={saving || !name.trim()}
            sx={{ flexShrink: 0, minWidth: 120 }}
          >
            Add
          </Button>
        </Stack>
      </Card>

      {/* Branches list with images + assignment */}
      <Card sx={{ p: 2 }}>
        <Typography variant="subtitle1" gutterBottom>
          Branches
        </Typography>
        <Divider sx={{ mb: 1.5 }} />
        {branches.length === 0 ? (
          <Typography color="text.secondary" py={2} textAlign="center">
            No branches yet.
          </Typography>
        ) : (
          <Stack spacing={2}>
            {branches.map((b) => (
              <Box key={b.id} sx={{ border: "1px solid", borderColor: "divider", borderRadius: 2, p: 1.5 }}>
                <Stack direction="row" spacing={1.5} alignItems="center">
                  <Avatar
                    variant="rounded"
                    src={b.profileImageId ? api.imageUrl(b.profileImageId) : undefined}
                    sx={{ width: 48, height: 48, bgcolor: "rgba(0,255,157,.12)", color: "primary.main" }}
                  >
                    <ApartmentRoundedIcon />
                  </Avatar>
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography variant="subtitle2" noWrap>{b.name}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {b.area || "—"} · {b.description || "No description"}
                    </Typography>
                  </Box>
                </Stack>

                {/* Image uploads */}
                <Stack direction="row" spacing={1} mt={1.5} flexWrap="wrap" useFlexGap>
                  {(["profile", "banner"] as const).map((kind) => (
                    <Box key={kind}>
                      <input
                        ref={(el) => (imgRefs.current[`${b.id}-${kind}`] = el)}
                        type="file"
                        accept="image/*"
                        hidden
                        onChange={(e) => uploadImage(b.id, kind, e.target.files?.[0] ?? null)}
                      />
                      <Button
                        size="small"
                        variant="outlined"
                        startIcon={<PhotoCameraRoundedIcon />}
                        onClick={() => imgRefs.current[`${b.id}-${kind}`]?.click()}
                        sx={{ textTransform: "none" }}
                      >
                        {kind === "profile" ? "Profile image" : "Banner image"}
                      </Button>
                    </Box>
                  ))}
                </Stack>

                {/* Assign admin + station */}
                <Stack direction={{ xs: "column", sm: "row" }} spacing={1} mt={1.5}>
                  <TextField
                    select
                    size="small"
                    label="Assign admin"
                    value=""
                    onChange={(e) => api.assignAdminToBranch(b.id, e.target.value).then(load)}
                    sx={{ flex: 1, minWidth: 160 }}
                  >
                    {admins.map((a) => (
                      <MenuItem key={a.id} value={a.id}>
                        {a.name} ({a.email})
                      </MenuItem>
                    ))}
                  </TextField>
                  <TextField
                    select
                    size="small"
                    label="Assign station"
                    value=""
                    onChange={(e) => api.assignStationToBranch(b.id, e.target.value).then(load)}
                    sx={{ flex: 1, minWidth: 160 }}
                  >
                    {stations.map((s) => (
                      <MenuItem key={s.id} value={s.id}>
                        {s.name}
                      </MenuItem>
                    ))}
                  </TextField>
                </Stack>

                {/* Members */}
                <Stack direction="row" spacing={0.5} mt={1} flexWrap="wrap" useFlexGap>
                  {admins.filter((a) => a.branchId === b.id).map((a) => (
                    <Chip key={a.id} size="small" label={a.name} />
                  ))}
                  {stations.filter((s) => (s as StationDetail & { branchId?: string }).branchId === b.id).length > 0 && (
                    <Chip
                      size="small"
                      color="primary"
                      variant="outlined"
                      label={`${stations.filter((s) => (s as StationDetail & { branchId?: string }).branchId === b.id).length} stations`}
                    />
                  )}
                </Stack>
              </Box>
            ))}
          </Stack>
        )}
      </Card>

      {/* Price study */}
      <Card sx={{ p: 2 }}>
        <Stack direction="row" spacing={1} alignItems="center" justifyContent="space-between" mb={1}>
          <Stack direction="row" spacing={1} alignItems="center">
            <TrendingUpRoundedIcon color="primary" />
            <Typography variant="subtitle1">Price study — changes by branch</Typography>
          </Stack>
          <TextField
            select
            size="small"
            label="Window"
            value={trendDays}
            onChange={(e) => setTrendDays(Number(e.target.value))}
            sx={{ width: 130 }}
          >
            <MenuItem value={1}>Today</MenuItem>
            <MenuItem value={7}>This week</MenuItem>
            <MenuItem value={30}>30 days</MenuItem>
          </TextField>
        </Stack>
        <Divider sx={{ mb: 1.5 }} />
        {trend.length === 0 ? (
          <Typography color="text.secondary" py={2}>No price changes in this window.</Typography>
        ) : (
          <Stack spacing={1.25}>
            {trend.map((t) => (
              <Box key={t.branchId ?? "none"}>
                <Stack direction="row" justifyContent="space-between">
                  <Typography variant="body2" fontWeight={600} noWrap>{t.branchName}</Typography>
                  <Typography variant="body2" color="text.secondary">{t.priceChanges}</Typography>
                </Stack>
                <LinearProgress
                  variant="determinate"
                  value={(t.priceChanges / maxTrend) * 100}
                  sx={{ mt: 0.5, height: 6, borderRadius: 3 }}
                />
              </Box>
            ))}
          </Stack>
        )}
      </Card>

      {/* Station update-frequency study */}
      <Card sx={{ p: 2 }}>
        <Typography variant="subtitle1" gutterBottom>
          Station update frequency
        </Typography>
        <Divider sx={{ mb: 1.5 }} />
        <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5} alignItems="center">
          <TextField
            select
            size="small"
            label="Station"
            value={freqStationId}
            onChange={(e) => setFreqStationId(e.target.value)}
            sx={{ flex: 1, minWidth: 200 }}
          >
            {stations.map((s) => (
              <MenuItem key={s.id} value={s.id}>
                {s.name}
              </MenuItem>
            ))}
          </TextField>
          <Button variant="outlined" onClick={lookupFrequency} disabled={!freqStationId}>
            Check (30 days)
          </Button>
        </Stack>
        {freq && (
          <Alert severity="info" sx={{ mt: 1.5 }}>
            {freq}
          </Alert>
        )}
      </Card>
    </Stack>
  );
}
