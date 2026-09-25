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
import type { AdminStats, ReportFeedItem, StationDetail } from "../api/types";
import { statusDot, statusLabel } from "../theme";
import DashboardShell from "./DashboardShell";
import UsersPanel from "./panels/UsersPanel";
import PricingTrendsPanel from "./panels/PricingTrendsPanel";
import SubmissionsPanel from "./panels/SubmissionsPanel";
import RewardsPanel from "./panels/RewardsPanel";
import StationMap from "../components/StationMap";
import AdminStationEditDialog from "../components/AdminStationEditDialog";

const CEBU: [number, number] = [10.3181, 123.9068];

function StatCard({ icon, label, value }: { icon: ReactNode; label: string; value: ReactNode }) {
  return (
    <Card sx={{ p: 2, display: "flex", alignItems: "center", gap: 1.5 }}>
      <Box
        sx={{
          width: 44,
          height: 44,
          borderRadius: "50%",
          bgcolor: "rgba(31,157,87,.12)",
          color: "primary.main",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          flexShrink: 0,
        }}
      >
        {icon}
      </Box>
      <Box sx={{ minWidth: 0 }}>
        <Typography variant="h5">{value}</Typography>
        <Typography variant="caption" color="text.secondary">
          {label}
        </Typography>
      </Box>
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
      {stats && (
        <Box
          sx={{
            display: "grid",
            gridTemplateColumns: { xs: "1fr 1fr", sm: "repeat(3, 1fr)", lg: "repeat(6, 1fr)" },
            gap: 2,
          }}
        >
          <StatCard icon={<EvStationRoundedIcon />} label="Stations" value={stats.totalStations} />
          <StatCard icon={<PowerRoundedIcon />} label="Chargers" value={stats.totalChargers} />
          <StatCard icon={<BoltRoundedIcon />} label="Available" value={stats.availableChargers} />
          <StatCard icon={<PeopleRoundedIcon />} label="Users" value={stats.totalUsers} />
          <StatCard icon={<FlagRoundedIcon />} label="Reports" value={stats.totalReports} />
          <StatCard icon={<WarningRoundedIcon />} label="Stale data" value={stats.staleStations} />
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
          <Typography variant="subtitle1" gutterBottom>
            Recent reports
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
          <Typography variant="subtitle1" gutterBottom>
            Station moderation
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
        <Typography variant="subtitle1" gutterBottom>
          Station map
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

export default function AdminApp() {
  return (
    <DashboardShell
      title="ChargeMap Admin"
      roleLabel="ADMIN"
      items={[
        { label: "Overview", icon: <DashboardRoundedIcon />, render: () => <Overview /> },
        { label: "Submissions", icon: <InboxRoundedIcon />, render: () => <SubmissionsPanel /> },
        { label: "Users", icon: <PeopleRoundedIcon />, render: () => <UsersPanel /> },
        { label: "Rewards", icon: <EmojiEventsRoundedIcon />, render: () => <RewardsPanel /> },
        { label: "Pricing trends", icon: <TrendingUpRoundedIcon />, render: () => <PricingTrendsPanel /> },
      ]}
    />
  );
}
