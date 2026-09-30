import { useEffect, useState, type ReactNode } from "react";
import {
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  Divider,
  Stack,
  Typography,
} from "@mui/material";
import DashboardRoundedIcon from "@mui/icons-material/DashboardRounded";
import InboxRoundedIcon from "@mui/icons-material/InboxRounded";
import PeopleRoundedIcon from "@mui/icons-material/PeopleRounded";
import EmojiEventsRoundedIcon from "@mui/icons-material/EmojiEventsRounded";
import TrendingUpRoundedIcon from "@mui/icons-material/TrendingUpRounded";
import EvStationRoundedIcon from "@mui/icons-material/EvStationRounded";
import PowerRoundedIcon from "@mui/icons-material/PowerRounded";
import BoltRoundedIcon from "@mui/icons-material/BoltRounded";
import FlagRoundedIcon from "@mui/icons-material/FlagRounded";
import WarningRoundedIcon from "@mui/icons-material/WarningAmberRounded";
import VerifiedRoundedIcon from "@mui/icons-material/VerifiedRounded";
import BlockRoundedIcon from "@mui/icons-material/BlockRounded";
import { api } from "../api/client";
import type { AdminStats, GridTelemetry, ReportFeedItem, StationDetail } from "../api/types";
import { hud, statusDot, statusLabel } from "../theme";
import DashboardShell from "./DashboardShell";
import UsersPanel from "./panels/UsersPanel";
import PricingTrendsPanel from "./panels/PricingTrendsPanel";
import SubmissionsPanel from "./panels/SubmissionsPanel";
import RewardsPanel from "./panels/RewardsPanel";
import StationMap from "../components/StationMap";
import AdminStationEditDialog from "../components/AdminStationEditDialog";
import BranchesPanel from "./panels/BranchesPanel";
import AdminManagementPanel from "./panels/AdminManagementPanel";
import AdminProfilePanel from "./panels/AdminProfilePanel";
import ApartmentRoundedIcon from "@mui/icons-material/ApartmentRounded";
import SupervisorAccountRoundedIcon from "@mui/icons-material/SupervisorAccountRounded";
import AccountCircleRoundedIcon from "@mui/icons-material/AccountCircleRounded";
import { useAuth } from "../auth/AuthContext";

const CEBU: [number, number] = [10.3181, 123.9068];

function StatCard({
  icon,
  label,
  value,
  accent = hud.mint,
}: {
  icon: ReactNode;
  label: string;
  value: ReactNode;
  accent?: string;
}) {
  return (
    <Card sx={{ p: 2, position: "relative", overflow: "hidden" }}>
      {/* Corner reticle accent */}
      <Box
        sx={{
          position: "absolute",
          top: 0,
          right: 0,
          width: 10,
          height: 10,
          borderTop: `2px solid ${accent}`,
          borderRight: `2px solid ${accent}`,
          opacity: 0.5,
        }}
      />
      <Stack direction="row" alignItems="center" spacing={1.5}>
        <Box
          sx={{
            width: 40,
            height: 40,
            borderRadius: 1,
            bgcolor: `${accent}1f`,
            color: accent,
            border: `1px solid ${accent}44`,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            flexShrink: 0,
          }}
        >
          {icon}
        </Box>
        <Box sx={{ minWidth: 0 }}>
          <Typography
            sx={{
              fontFamily: "'JetBrains Mono', monospace",
              fontWeight: 700,
              fontSize: 24,
              lineHeight: 1,
              color: hud.textHigh,
            }}
          >
            {value}
          </Typography>
          <Typography
            sx={{
              fontFamily: "'JetBrains Mono', monospace",
              fontSize: 9,
              fontWeight: 700,
              letterSpacing: "0.12em",
              textTransform: "uppercase",
              color: hud.textMuted,
              mt: 0.5,
            }}
          >
            {label}
          </Typography>
        </Box>
      </Stack>
    </Card>
  );
}

function timeAgo(iso: string): string {
  const mins = Math.round((Date.now() - new Date(iso).getTime()) / 60000);
  if (mins < 1) return "just now";
  if (mins < 60) return `${mins}m ago`;
  const h = Math.round(mins / 60);
  if (h < 24) return `${h}h ago`;
  return `${Math.round(h / 24)}d ago`;
}

function Overview() {
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [reports, setReports] = useState<ReportFeedItem[]>([]);
  const [stations, setStations] = useState<StationDetail[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [editStation, setEditStation] = useState<StationDetail | null>(null);

  const load = async () => {
    setLoading(true);
    try {
      const [s, r, st] = await Promise.all([
        api.adminStats(),
        api.adminReports(25),
        api.adminStations(),
      ]);
      setStats(s);
      setReports(r);
      setStations(st);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const patch = (u: StationDetail) =>
    setStations((prev) => prev.map((s) => (s.id === u.id ? u : s)));

  const act = async (id: string, fn: () => Promise<StationDetail>) => {
    setBusyId(id);
    try {
      patch(await fn());
      setStats(await api.adminStats());
    } finally {
      setBusyId(null);
    }
  };

  if (loading) {
    return (
      <Stack alignItems="center" py={6}>
        <CircularProgress />
      </Stack>
    );
  }

  return (
    <Stack spacing={3}>
      {/* Top telemetry status strip */}
      <Box
        sx={{
          display: "flex",
          alignItems: "center",
          gap: 2,
          flexWrap: "wrap",
          px: 2,
          py: 1,
          borderRadius: 1,
          bgcolor: hud.surface1,
          border: `1px solid ${hud.border}`,
          fontFamily: "'JetBrains Mono', monospace",
        }}
      >
        <Stack direction="row" spacing={0.75} alignItems="center">
          <Box
            sx={{
              width: 8,
              height: 8,
              borderRadius: "50%",
              bgcolor: hud.mint,
              boxShadow: "0 0 8px rgba(0,255,157,.7)",
            }}
          />
          <Typography sx={{ fontFamily: "inherit", fontSize: 11, fontWeight: 700, letterSpacing: "0.1em", color: hud.mint }}>
            GRID OPERATIONAL
          </Typography>
        </Stack>
        {stats && (
          <>
            <Typography sx={{ fontFamily: "inherit", fontSize: 11, color: hud.textMuted }}>
              STATIONS <Box component="span" sx={{ color: hud.textHigh, fontWeight: 700 }}>{stats.totalStations}</Box>
            </Typography>
            <Typography sx={{ fontFamily: "inherit", fontSize: 11, color: hud.textMuted }}>
              LIVE BAYS{" "}
              <Box component="span" sx={{ color: hud.textHigh, fontWeight: 700 }}>
                {stats.availableChargers}/{stats.totalChargers}
              </Box>
            </Typography>
            <Typography sx={{ fontFamily: "inherit", fontSize: 11, color: hud.textMuted }}>
              PENDING{" "}
              <Box component="span" sx={{ color: stats.totalReports > 0 ? hud.amber : hud.textHigh, fontWeight: 700 }}>
                {stats.totalReports}
              </Box>
            </Typography>
          </>
        )}
        <Box sx={{ flex: 1 }} />
        <Typography sx={{ fontFamily: "inherit", fontSize: 10, letterSpacing: "0.1em", color: hud.textMuted }}>
          ● SYNCED : SECURE
        </Typography>
      </Box>

      <GridTelemetryCard />

      {stats && (
        <Box
          sx={{
            display: "grid",
            gridTemplateColumns: { xs: "1fr 1fr", sm: "repeat(3, 1fr)", lg: "repeat(6, 1fr)" },
            gap: 2,
          }}
        >
          <StatCard icon={<EvStationRoundedIcon />} label="Stations" value={stats.totalStations} accent={hud.mint} />
          <StatCard icon={<PowerRoundedIcon />} label="Chargers" value={stats.totalChargers} accent={hud.cyan} />
          <StatCard icon={<BoltRoundedIcon />} label="Available" value={stats.availableChargers} accent={hud.mint} />
          <StatCard icon={<PeopleRoundedIcon />} label="Users" value={stats.totalUsers} accent={hud.cyan} />
          <StatCard icon={<FlagRoundedIcon />} label="Reports" value={stats.totalReports} accent={hud.amber} />
          <StatCard icon={<WarningRoundedIcon />} label="Stale data" value={stats.staleStations} accent={hud.neon} />
        </Box>
      )}

      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: { xs: "1fr", lg: "1fr 1.4fr" },
          gap: 3,
          alignItems: "start",
        }}
      >
        <Card sx={{ p: 2 }}>
          <Typography
            sx={{
              fontFamily: "'JetBrains Mono', monospace",
              fontSize: 12,
              fontWeight: 700,
              letterSpacing: "0.1em",
              textTransform: "uppercase",
              color: hud.textHigh,
            }}
            gutterBottom
          >
            ▚ Community Queue &amp; Reports
          </Typography>
          <Divider sx={{ mb: 1 }} />
          {reports.length === 0 ? (
            <Typography variant="body2" color="text.secondary" py={2}>
              No reports yet.
            </Typography>
          ) : (
            <Stack divider={<Divider />} sx={{ maxHeight: 420, overflowY: "auto" }}>
              {reports.map((r) => (
                <Stack
                  key={r.reportId}
                  direction="row"
                  alignItems="center"
                  justifyContent="space-between"
                  sx={{ py: 1 }}
                >
                  <Box sx={{ minWidth: 0 }}>
                    <Typography variant="body2" fontWeight={600} noWrap>
                      {r.stationName}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {timeAgo(r.createdAt)}
                    </Typography>
                  </Box>
                  <Stack direction="row" spacing={0.5} alignItems="center">
                    <Box sx={{ width: 8, height: 8, borderRadius: "50%", bgcolor: statusDot(r.status) }} />
                    <Typography variant="caption" sx={{ color: statusDot(r.status), fontWeight: 600 }}>
                      {statusLabel(r.status)}
                    </Typography>
                  </Stack>
                </Stack>
              ))}
            </Stack>
          )}
        </Card>

        <Card sx={{ p: 2 }}>
          <Typography
            sx={{
              fontFamily: "'JetBrains Mono', monospace",
              fontSize: 12,
              fontWeight: 700,
              letterSpacing: "0.1em",
              textTransform: "uppercase",
              color: hud.textHigh,
            }}
            gutterBottom
          >
            ▚ Stations &amp; Hubs — Moderation
          </Typography>
          <Divider sx={{ mb: 1 }} />
          <Stack divider={<Divider />} sx={{ maxHeight: 420, overflowY: "auto" }}>
            {stations.map((s) => (
              <Stack
                key={s.id}
                direction={{ xs: "column", sm: "row" }}
                alignItems={{ xs: "stretch", sm: "center" }}
                justifyContent="space-between"
                spacing={1}
                sx={{ py: 1 }}
              >
                <Box sx={{ minWidth: 0 }}>
                  <Stack direction="row" spacing={0.75} alignItems="center" flexWrap="wrap" useFlexGap>
                    <Typography variant="body2" fontWeight={600} noWrap>
                      {s.name}
                    </Typography>
                    {s.disabled && <Chip label="Disabled" size="small" color="error" />}
                    {s.confidence && <Chip label={s.confidence} size="small" variant="outlined" />}
                  </Stack>
                  <Typography variant="caption" color="text.secondary">
                    {s.dataSource ?? "—"} · {s.availableCount}/{s.totalChargers} available
                  </Typography>
                </Box>
                <Stack direction="row" spacing={0.5} sx={{ flexShrink: 0 }} flexWrap="wrap" useFlexGap>
                  <Button
                    size="small"
                    disabled={busyId === s.id}
                    onClick={() => setEditStation(s)}
                  >
                    Edit
                  </Button>
                  <Button
                    size="small"
                    startIcon={<VerifiedRoundedIcon />}
                    disabled={busyId === s.id}
                    onClick={() => act(s.id, () => api.adminVerifyStation(s.id))}
                  >
                    Verify
                  </Button>
                  {s.disabled ? (
                    <Button
                      size="small"
                      color="success"
                      disabled={busyId === s.id}
                      onClick={() => act(s.id, () => api.adminEnableStation(s.id))}
                    >
                      Enable
                    </Button>
                  ) : (
                    <Button
                      size="small"
                      color="error"
                      startIcon={<BlockRoundedIcon />}
                      disabled={busyId === s.id}
                      onClick={() => act(s.id, () => api.adminDisableStation(s.id))}
                    >
                      Disable
                    </Button>
                  )}
                </Stack>
              </Stack>
            ))}
          </Stack>
        </Card>
      </Box>

      {/* Station map — click any pin to edit that station's details. */}
      <Card sx={{ p: 2 }}>
        <Typography
          sx={{
            fontFamily: "'JetBrains Mono', monospace",
            fontSize: 12,
            fontWeight: 700,
            letterSpacing: "0.1em",
            textTransform: "uppercase",
            color: hud.textHigh,
          }}
          gutterBottom
        >
          ▚ Live Network Map
        </Typography>
        <Typography variant="caption" color="text.secondary">
          Tap a station pin to edit its details.
        </Typography>
        <Box
          sx={{
            mt: 1.5,
            height: { xs: 320, md: 460 },
            borderRadius: 2,
            overflow: "hidden",
          }}
        >
          <StationMap
            center={
              stations.find((s) => s.location)?.location
                ? [
                    stations.find((s) => s.location)!.location!.lat,
                    stations.find((s) => s.location)!.location!.lng,
                  ]
                : CEBU
            }
            stations={stations}
            selectedId={editStation?.id ?? null}
            onSelect={(id) => {
              const s = stations.find((x) => x.id === id);
              if (s) setEditStation(s);
            }}
          />
        </Box>
      </Card>

      <LikeTrendCard />

      <AdminStationEditDialog
        station={editStation}
        onClose={() => setEditStation(null)}
        onSaved={(u) => {
          patch(u);
          setEditStation(null);
        }}
      />
    </Stack>
  );
}

/**
 * SIMULATED grid throughput + IoT hardware stream for the Telemetry Hub. There is no real
 * charger-hardware feed (no OCPP/OCPI integration), so every value here is synthesized and
 * clearly labeled DEMO. Polls the backend every 10s for fresh simulated values.
 */
function GridTelemetryCard() {
  const [grid, setGrid] = useState<GridTelemetry | null>(null);

  useEffect(() => {
    let active = true;
    const fetchGrid = () =>
      api
        .gridTelemetry()
        .then((g) => {
          if (active) setGrid(g);
        })
        .catch(() => {});
    fetchGrid();
    const id = setInterval(fetchGrid, 10000);
    return () => {
      active = false;
      clearInterval(id);
    };
  }, []);

  const metric = (label: string, value: string, unit?: string, accent: string = hud.mint) => (
    <Box sx={{ bgcolor: "rgba(0,0,0,.3)", p: 1.25, borderRadius: 1, border: `1px solid ${hud.border}` }}>
      <Typography
        sx={{
          fontFamily: "'JetBrains Mono', monospace",
          fontSize: 9,
          fontWeight: 700,
          letterSpacing: "0.12em",
          textTransform: "uppercase",
          color: hud.textMuted,
        }}
      >
        {label}
      </Typography>
      <Typography
        sx={{ fontFamily: "'JetBrains Mono', monospace", fontWeight: 700, fontSize: 18, color: accent, mt: 0.25 }}
      >
        {value}
        {unit && (
          <Typography component="span" sx={{ fontSize: 10, color: hud.textMuted, ml: 0.4 }}>
            {unit}
          </Typography>
        )}
      </Typography>
    </Box>
  );

  const levelColor = (lvl: string) =>
    lvl === "ERROR" ? hud.neon : lvl === "WARN" ? hud.amber : hud.mint;

  return (
    <Card sx={{ p: 2 }}>
      <Stack direction="row" alignItems="center" justifyContent="space-between" mb={1}>
        <Typography
          sx={{
            fontFamily: "'JetBrains Mono', monospace",
            fontSize: 12,
            fontWeight: 700,
            letterSpacing: "0.1em",
            textTransform: "uppercase",
            color: hud.textHigh,
          }}
        >
          ▚ IoT Hardware Stream
        </Typography>
        <Chip
          label="SIMULATED · DEMO"
          size="small"
          sx={{
            height: 20,
            bgcolor: "rgba(0,184,255,.1)",
            color: hud.cyan,
            border: `1px solid ${hud.cyan}55`,
            "& .MuiChip-label": { px: 0.75, fontSize: 9, letterSpacing: "0.08em" },
          }}
        />
      </Stack>
      <Typography variant="caption" sx={{ color: hud.textMuted, display: "block", mb: 1.5 }}>
        No live charger-hardware feed exists (requires OCPP/OCPI operator integration). These
        readings are synthesized for demonstration only.
      </Typography>

      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: { xs: "1fr 1fr", sm: "repeat(5, 1fr)" },
          gap: 1.25,
          mb: 2,
        }}
      >
        {metric("Throughput", grid ? String(grid.throughputMw) : "—", "MW", hud.mint)}
        {metric("Capacity", grid ? String(grid.capacityPercent) : "—", "%", hud.cyan)}
        {metric("Grid Freq", grid ? String(grid.gridFrequencyHz) : "—", "Hz", hud.mint)}
        {metric("Thermistor", grid ? String(grid.thermistorC) : "—", "°C", hud.amber)}
        {metric("RFID Latency", grid ? String(grid.rfidLatencyMs) : "—", "ms", hud.cyan)}
      </Box>

      {/* IoT log stream */}
      <Box
        sx={{
          bgcolor: "#05070b",
          border: `1px solid ${hud.border}`,
          borderRadius: 1,
          p: 1.25,
          maxHeight: 200,
          overflowY: "auto",
          fontFamily: "'JetBrains Mono', monospace",
        }}
      >
        {(grid?.iotStream ?? []).map((line, i) => (
          <Box key={i} sx={{ display: "flex", gap: 1, fontSize: 11, py: 0.25 }}>
            <Box component="span" sx={{ color: hud.textMuted, flexShrink: 0 }}>
              [{new Date(line.timestamp).toLocaleTimeString("en-GB")}]
            </Box>
            <Box component="span" sx={{ color: hud.cyan, flexShrink: 0 }}>
              {line.source}
            </Box>
            <Box component="span" sx={{ color: levelColor(line.level) }}>
              {line.message}
            </Box>
          </Box>
        ))}
        {!grid && (
          <Typography sx={{ fontSize: 11, color: hud.textMuted }}>Connecting…</Typography>
        )}
      </Box>
    </Card>
  );
}

/** Like trend for the admin's assigned branch (super admin sees all). */
function LikeTrendCard() {
  const [points, setPoints] = useState<import("../api/types").LikeTrendPoint[]>([]);
  const [days, setDays] = useState(7);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    api
      .branchLikeTrend(days)
      .then(setPoints)
      .catch(() => setPoints([]))
      .finally(() => setLoaded(true));
  }, [days]);

  const max = points.reduce((m, p) => Math.max(m, p.likes), 0) || 1;
  const totalNet = points.reduce((s, p) => s + p.net, 0);

  return (
    <Card sx={{ p: 2 }}>
      <Stack direction="row" alignItems="center" justifyContent="space-between" mb={1}>
        <Typography
          sx={{
            fontFamily: "'JetBrains Mono', monospace",
            fontSize: 12,
            fontWeight: 700,
            letterSpacing: "0.1em",
            textTransform: "uppercase",
            color: hud.textHigh,
          }}
        >
          ▚ Station Like Trend
        </Typography>
        <Chip label={`${totalNet >= 0 ? "+" : ""}${totalNet} net`} size="small" color={totalNet >= 0 ? "success" : "default"} />
      </Stack>
      <Divider sx={{ mb: 1.5 }} />
      <Stack direction="row" spacing={1} mb={1.5}>
        {[
          { d: 1, label: "Today" },
          { d: 7, label: "Week" },
          { d: 30, label: "30 days" },
        ].map((o) => (
          <Chip
            key={o.d}
            label={o.label}
            size="small"
            color={days === o.d ? "primary" : "default"}
            variant={days === o.d ? "filled" : "outlined"}
            onClick={() => setDays(o.d)}
          />
        ))}
      </Stack>
      {!loaded ? (
        <Typography variant="caption" color="text.secondary">
          Loading…
        </Typography>
      ) : points.length === 0 ? (
        <Typography variant="body2" color="text.secondary" py={1}>
          No likes recorded in this window for your branch's stations.
        </Typography>
      ) : (
        <Stack direction="row" alignItems="flex-end" spacing={1} sx={{ height: 120, mt: 1 }}>
          {points.map((p) => (
            <Box key={p.date} sx={{ flex: 1, textAlign: "center" }}>
              <Box
                sx={{
                  height: `${(p.likes / max) * 90}px`,
                  minHeight: 3,
                  bgcolor: "primary.main",
                  borderRadius: 1,
                  mx: "auto",
                  width: "70%",
                }}
                title={`${p.likes} likes, ${p.unlikes} unlikes`}
              />
              <Typography sx={{ fontSize: 9, mt: 0.5 }} color="text.secondary">
                {p.date.slice(5)}
              </Typography>
            </Box>
          ))}
        </Stack>
      )}
    </Card>
  );
}

export default function AdminApp() {
  const { user } = useAuth();
  const isSuper = user?.role === "SUPER_ADMIN";

  const items = [
    { label: "Overview", icon: <DashboardRoundedIcon />, render: () => <Overview /> },
    { label: "Submissions", icon: <InboxRoundedIcon />, render: () => <SubmissionsPanel /> },
    { label: "Users", icon: <PeopleRoundedIcon />, render: () => <UsersPanel /> },
    { label: "Rewards", icon: <EmojiEventsRoundedIcon />, render: () => <RewardsPanel /> },
    { label: "Pricing trends", icon: <TrendingUpRoundedIcon />, render: () => <PricingTrendsPanel /> },
    // Super-admin-only areas.
    ...(isSuper
      ? [
          { label: "Branches", icon: <ApartmentRoundedIcon />, render: () => <BranchesPanel /> },
          {
            label: "Admins",
            icon: <SupervisorAccountRoundedIcon />,
            render: () => <AdminManagementPanel />,
          },
        ]
      : []),
    { label: "Profile", icon: <AccountCircleRoundedIcon />, render: () => <AdminProfilePanel /> },
  ];

  return (
    <DashboardShell
      title={isSuper ? "ChargeMap Super Admin" : "ChargeMap Admin"}
      roleLabel={isSuper ? "SUPER ADMIN" : "ADMIN"}
      items={items}
    />
  );
}
