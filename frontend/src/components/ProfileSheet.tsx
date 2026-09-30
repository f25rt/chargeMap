import { useEffect, useState } from "react";
import {
  Avatar,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  Divider,
  IconButton,
  LinearProgress,
  Stack,
  Tab,
  Tabs,
  Typography,
} from "@mui/material";
import CloseRoundedIcon from "@mui/icons-material/CloseRounded";
import EmojiEventsRoundedIcon from "@mui/icons-material/EmojiEventsRounded";
import StarRoundedIcon from "@mui/icons-material/StarRounded";
import LogoutRoundedIcon from "@mui/icons-material/LogoutRounded";
import BoltRoundedIcon from "@mui/icons-material/BoltRounded";
import EditRoundedIcon from "@mui/icons-material/EditRounded";
import LockRoundedIcon from "@mui/icons-material/LockRounded";
import { hud } from "../theme";
import VehiclePanel from "./VehiclePanel";
import ActivitiesPanel from "./ActivitiesPanel";
import { api } from "../api/client";
import type { ChargingSession, PointsLedgerEntry, Prize, TelemetryRollup } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { TextField } from "@mui/material";

interface Props {
  open: boolean;
  onClose: () => void;
}



/** Small uppercase telemetry label. */
function MicroLabel({ children }: { children: React.ReactNode }) {
  return (
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
      {children}
    </Typography>
  );
}

/** A boxed telemetry readout used in the secondary stat grid. */
function TelemetryStat({
  label,
  value,
  unit,
  sub,
  accent = hud.textHigh,
}: {
  label: string;
  value: string;
  unit?: string;
  sub?: string;
  accent?: string;
}) {
  return (
    <Box
      sx={{
        bgcolor: "rgba(0,0,0,.3)",
        p: 1.25,
        borderRadius: 1,
        border: `1px solid ${hud.border}`,
      }}
    >
      <MicroLabel>{label}</MicroLabel>
      <Typography
        sx={{
          fontFamily: "'JetBrains Mono', monospace",
          fontWeight: 700,
          fontSize: 14,
          color: accent,
          mt: 0.25,
        }}
      >
        {value}
        {unit && (
          <Typography component="span" sx={{ fontSize: 11, color: hud.textMuted, ml: 0.5 }}>
            {unit}
          </Typography>
        )}
      </Typography>
      {sub && (
        <Typography sx={{ fontSize: 9, color: hud.textMuted, mt: 0.25 }}>{sub}</Typography>
      )}
    </Box>
  );
}

const TASK_LABEL_MAP: Record<string, string> = {
  STATION_ADD: "Added a station",
  STATION_UPDATE: "Updated a station",
  REPORT: "Reported status",
  ADMIN_ADJUST: "Adjustment",
};

/** Session History tab: log a charging session + view past sessions and points ledger. */
function SessionsTab({
  sessions,
  ledger,
  onLogged,
}: {
  sessions: ChargingSession[];
  ledger: PointsLedgerEntry[];
  onLogged: () => void;
}) {
  const [open, setOpen] = useState(false);
  const [station, setStation] = useState("");
  const [energy, setEnergy] = useState("");
  const [duration, setDuration] = useState("");
  const [peak, setPeak] = useState("");
  const [price, setPrice] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const submit = async () => {
    const kwh = Number(energy);
    if (!kwh || kwh <= 0) {
      setErr("Enter the energy delivered (kWh).");
      return;
    }
    setBusy(true);
    setErr(null);
    try {
      await api.logSession({
        stationName: station.trim() || undefined,
        energyKwh: kwh,
        durationMinutes: duration ? Number(duration) : undefined,
        peakKw: peak ? Number(peak) : undefined,
        pricePerKwh: price ? Number(price) : undefined,
      });
      setStation("");
      setEnergy("");
      setDuration("");
      setPeak("");
      setPrice("");
      setOpen(false);
      onLogged();
    } catch {
      setErr("Could not log the session. Try again.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Stack spacing={1.5}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <MicroLabel>Session History</MicroLabel>
        <Button
          size="small"
          variant={open ? "text" : "outlined"}
          onClick={() => setOpen((o) => !o)}
        >
          {open ? "Cancel" : "+ Log Session"}
        </Button>
      </Stack>

      {open && (
        <Box
          sx={{
            p: 1.75,
            borderRadius: 1,
            bgcolor: hud.surface2,
            border: `1px solid ${hud.borderMint}`,
          }}
        >
          {err && (
            <Typography sx={{ color: hud.neon, fontSize: 12, mb: 1 }}>{err}</Typography>
          )}
          <Stack spacing={1.25}>
            <TextField
              size="small"
              label="Station (optional)"
              value={station}
              onChange={(e) => setStation(e.target.value)}
              fullWidth
            />
            <Stack direction="row" spacing={1}>
              <TextField
                size="small"
                type="number"
                label="Energy (kWh) *"
                value={energy}
                onChange={(e) => setEnergy(e.target.value)}
                sx={{ flex: 1 }}
              />
              <TextField
                size="small"
                type="number"
                label="Duration (min)"
                value={duration}
                onChange={(e) => setDuration(e.target.value)}
                sx={{ flex: 1 }}
              />
            </Stack>
            <Stack direction="row" spacing={1}>
              <TextField
                size="small"
                type="number"
                label="Peak (kW)"
                value={peak}
                onChange={(e) => setPeak(e.target.value)}
                sx={{ flex: 1 }}
              />
              <TextField
                size="small"
                type="number"
                label="Price (₱/kWh)"
                value={price}
                onChange={(e) => setPrice(e.target.value)}
                sx={{ flex: 1 }}
              />
            </Stack>
            <Button variant="contained" onClick={submit} disabled={busy}>
              {busy ? "Logging…" : "Log Session"}
            </Button>
          </Stack>
        </Box>
      )}

      {sessions.length === 0 ? (
        <Typography color="text.secondary" py={1} textAlign="center" sx={{ fontSize: 13 }}>
          No sessions logged yet. Log your first charge to track energy &amp; CO2.
        </Typography>
      ) : (
        <Stack spacing={1}>
          {sessions.map((s) => (
            <Box
              key={s.id}
              sx={{
                p: 1.25,
                borderRadius: 1,
                bgcolor: hud.surface2,
                border: `1px solid ${hud.border}`,
              }}
            >
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Typography variant="body2" fontWeight={600} noWrap sx={{ color: hud.textHigh }}>
                  {s.stationName ?? "Session"}
                </Typography>
                <Typography
                  sx={{ fontFamily: "'JetBrains Mono', monospace", fontWeight: 700, color: hud.mint, fontSize: 13 }}
                >
                  {s.energyKwh} kWh
                </Typography>
              </Stack>
              <Stack
                direction="row"
                spacing={1.5}
                sx={{ mt: 0.5, fontFamily: "'JetBrains Mono', monospace", color: hud.textMuted, fontSize: 11 }}
                flexWrap="wrap"
                useFlexGap
              >
                {s.cost != null && <span>₱{s.cost}</span>}
                {s.durationMinutes != null && <span>• {s.durationMinutes} min</span>}
                {s.peakKw != null && <span>• {s.peakKw} kW peak</span>}
                <span>• {s.co2SavedKg} kg CO2 saved</span>
                {s.startedAt && <span>• {new Date(s.startedAt).toLocaleDateString()}</span>}
              </Stack>
            </Box>
          ))}
        </Stack>
      )}

      {ledger.length > 0 && (
        <>
          <MicroLabel>Points Ledger</MicroLabel>
          <Stack divider={<Divider />}>
            {ledger.map((e, i) => (
              <Stack
                key={i}
                direction="row"
                justifyContent="space-between"
                alignItems="center"
                sx={{ py: 0.75 }}
              >
                <Typography variant="caption" sx={{ color: hud.textMuted }}>
                  {TASK_LABEL_MAP[e.taskType] ?? e.taskType}
                </Typography>
                <Typography
                  variant="body2"
                  fontWeight={700}
                  color={e.points >= 0 ? "success.main" : "error.main"}
                >
                  {e.points >= 0 ? "+" : ""}
                  {e.points}
                </Typography>
              </Stack>
            ))}
          </Stack>
        </>
      )}
    </Stack>
  );
}

export default function ProfileSheet({ open, onClose }: Props) {
  const { user, refresh, logout } = useAuth();
  const [tab, setTab] = useState(0);
  const [ledger, setLedger] = useState<PointsLedgerEntry[]>([]);
  const [prizes, setPrizes] = useState<Prize[]>([]);
  const [sessions, setSessions] = useState<ChargingSession[]>([]);
  const [rollup, setRollup] = useState<TelemetryRollup | null>(null);
  const [loading, setLoading] = useState(false);

  const reload = () => {
    setLoading(true);
    Promise.all([refresh(), api.myPoints(), api.prizes(), api.mySessions(), api.myTelemetry()])
      .then(([, l, p, s, t]) => {
        setLedger(l);
        setPrizes(p);
        setSessions(s);
        setRollup(t);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (!open) return;
    reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const balance = user?.pointsBalance ?? 0;
  const initial = user?.name?.charAt(0).toUpperCase() ?? "?";

  // "Next tier" = cheapest prize the user can't yet afford (real data-driven target).
  const sortedPrizes = [...prizes].sort((a, b) => a.pointCost - b.pointCost);
  const nextTarget = sortedPrizes.find((p) => p.pointCost > balance);
  const nextTierPts = nextTarget?.pointCost ?? balance;
  const tierProgress = nextTarget
    ? Math.min(100, Math.round((balance / nextTierPts) * 100))
    : 100;

  return (
    <Dialog
      open={open}
      onClose={onClose}
      fullWidth
      maxWidth="sm"
      scroll="paper"
      PaperProps={{
        sx: {
          borderRadius: 2,
          m: 2,
          width: "100%",
          maxWidth: 620,
          maxHeight: "calc(100dvh - 32px)",
          display: "flex",
          flexDirection: "column",
          overflow: "hidden",
          border: `1px solid ${hud.border}`,
          bgcolor: hud.surface1,
        },
      }}
    >
      {/* Top HUD accent bar */}
      <Box
        sx={{
          height: 3,
          width: "100%",
          background: `linear-gradient(90deg, ${hud.cyan}, ${hud.mint}, ${hud.mintDim})`,
        }}
      />

      {/* Header bar */}
      <Stack
        direction="row"
        alignItems="center"
        justifyContent="space-between"
        sx={{
          px: 2.5,
          py: 1.75,
          borderBottom: `1px solid ${hud.border}`,
          bgcolor: "rgba(20,24,34,.4)",
        }}
      >
        <Stack direction="row" spacing={1.25} alignItems="center">
          <Box
            sx={{
              width: 10,
              height: 10,
              borderRadius: 0.5,
              bgcolor: hud.mint,
              boxShadow: "0 0 12px rgba(0,255,157,.6)",
            }}
          />
          <Box>
            <Stack direction="row" spacing={1} alignItems="center">
              <Typography variant="subtitle1" sx={{ color: hud.textHigh }}>
                Driver Telemetry &amp; Rewards
              </Typography>
              <Chip
                label="Sync Online"
                size="small"
                sx={{
                  height: 18,
                  bgcolor: "rgba(0,255,157,.10)",
                  color: hud.mint,
                  border: `1px solid rgba(0,255,157,.3)`,
                  "& .MuiChip-label": { px: 0.75, fontSize: 9 },
                }}
              />
            </Stack>
            <Typography
              sx={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 11, color: hud.textMuted }}
            >
              {user?.email ?? "—"} • Cebu EV Grid Network
            </Typography>
          </Box>
        </Stack>
        <Stack direction="row" spacing={0.5} alignItems="center">
          <Button
            size="small"
            color="inherit"
            startIcon={<LogoutRoundedIcon />}
            onClick={() => {
              logout();
              onClose();
            }}
            sx={{ color: hud.textMuted, fontSize: 11 }}
          >
            Disconnect
          </Button>
          <IconButton onClick={onClose} size="small" sx={{ bgcolor: hud.surface2 }}>
            <CloseRoundedIcon fontSize="small" />
          </IconButton>
        </Stack>
      </Stack>

      {/* Scrollable body */}
      <Box sx={{ px: 2.5, py: 2, overflowY: "auto", flex: 1, minHeight: 0 }}>
        {/* Hero telemetry card */}
        <Box
          sx={{
            position: "relative",
            borderRadius: 1.5,
            p: 2.5,
            overflow: "hidden",
            background: "linear-gradient(135deg,#131823,#0f141f 55%,#0d1017)",
            border: `1px solid ${hud.border}`,
          }}
        >
          <Typography
            sx={{
              position: "absolute",
              right: 12,
              top: 10,
              fontFamily: "'JetBrains Mono', monospace",
              fontSize: 9,
              letterSpacing: "0.12em",
              color: "rgba(139,155,180,.7)",
              textTransform: "uppercase",
            }}
          >
            Telemetry Node #CEB-01
          </Typography>

          <Stack
            direction={{ xs: "column", sm: "row" }}
            justifyContent="space-between"
            spacing={2}
            alignItems={{ sm: "center" }}
          >
            {/* Avatar + identity */}
            <Stack direction="row" spacing={2} alignItems="center">
              <Box sx={{ position: "relative" }}>
                <Avatar
                  variant="rounded"
                  sx={{
                    width: 60,
                    height: 60,
                    bgcolor: "#17202e",
                    color: hud.mint,
                    border: `2px solid ${hud.mint}`,
                    fontFamily: "'JetBrains Mono', monospace",
                    fontWeight: 700,
                    fontSize: 24,
                    boxShadow: "0 0 20px rgba(0,255,157,.35)",
                  }}
                >
                  {initial}
                </Avatar>
                <Chip
                  label={user?.level ?? "LVL 1"}
                  sx={{
                    position: "absolute",
                    bottom: -8,
                    right: -8,
                    height: 16,
                    bgcolor: hud.surface1,
                    color: hud.mint,
                    border: `1px solid ${hud.mint}`,
                    "& .MuiChip-label": { px: 0.5, fontSize: 8, fontWeight: 700 },
                  }}
                />
              </Box>
              <Box>
                <Stack direction="row" spacing={1} alignItems="center">
                  <Typography variant="h6" sx={{ color: hud.textHigh }}>
                    {user?.name}
                  </Typography>
                  <Chip
                    icon={<StarRoundedIcon sx={{ fontSize: 12, color: `${hud.mint} !important` }} />}
                    label="Fleet Pioneer"
                    size="small"
                    sx={{
                      height: 20,
                      bgcolor: "rgba(0,255,157,.10)",
                      color: hud.mint,
                      border: `1px solid rgba(0,255,157,.3)`,
                      "& .MuiChip-label": { px: 0.75, fontSize: 10 },
                    }}
                  />
                </Stack>
                <Typography variant="caption" sx={{ color: hud.textMuted }}>
                  Active EV pilot &amp; community surveyor
                </Typography>
                <Stack direction="row" spacing={1} mt={1} flexWrap="wrap" useFlexGap>
                  <Chip
                    icon={<BoltRoundedIcon sx={{ fontSize: 12, color: `${hud.mint} !important` }} />}
                    label={`${user?.stationsAdded ?? 0} stations added`}
                    size="small"
                    variant="outlined"
                    sx={{ height: 22, "& .MuiChip-label": { px: 0.75, fontSize: 10 } }}
                  />
                  <Chip
                    icon={<EditRoundedIcon sx={{ fontSize: 12, color: `${hud.cyan} !important` }} />}
                    label={`${user?.updatesMade ?? 0} logs & updates`}
                    size="small"
                    variant="outlined"
                    sx={{ height: 22, "& .MuiChip-label": { px: 0.75, fontSize: 10 } }}
                  />
                </Stack>
              </Box>
            </Stack>

            {/* Big points balance */}
            <Box
              sx={{
                bgcolor: "rgba(11,14,20,.7)",
                p: 1.5,
                borderRadius: 1.5,
                border: `1px solid ${hud.border}`,
                textAlign: { sm: "right" },
                minWidth: 150,
              }}
            >
              <Typography
                sx={{
                  fontFamily: "'JetBrains Mono', monospace",
                  fontWeight: 700,
                  fontSize: 30,
                  lineHeight: 1,
                  color: hud.textHigh,
                }}
              >
                {balance}
                <Typography component="span" sx={{ fontSize: 12, color: hud.mint, ml: 0.5 }}>
                  PTS
                </Typography>
              </Typography>
              <MicroLabel>Current Balance</MicroLabel>
              <Box sx={{ mt: 1 }}>
                <Typography sx={{ fontSize: 11, color: hud.textMuted, mb: 0.5 }}>
                  {nextTarget ? (
                    <>
                      Next tier at{" "}
                      <Box component="span" sx={{ color: hud.textHigh, fontWeight: 700 }}>
                        {nextTierPts} pts
                      </Box>
                    </>
                  ) : (
                    "Top tier reached"
                  )}
                </Typography>
                <LinearProgress
                  variant="determinate"
                  value={tierProgress}
                  sx={{
                    height: 6,
                    borderRadius: 1,
                    bgcolor: "rgba(30,41,59,.8)",
                    "& .MuiLinearProgress-bar": {
                      background: `linear-gradient(90deg,${hud.cyan},${hud.mint})`,
                    },
                  }}
                />
              </Box>
            </Box>
          </Stack>

          {/* Secondary telemetry stat grid (real, data-derived) */}
          <Box
            sx={{
              mt: 2,
              pt: 1.75,
              borderTop: `1px solid ${hud.border}`,
              display: "grid",
              gridTemplateColumns: { xs: "1fr 1fr", sm: "1fr 1fr 1fr" },
              gap: 1.25,
            }}
          >
            <TelemetryStat
              label="Energy Logged"
              value={rollup ? String(rollup.energyLoggedKwh) : "—"}
              unit="kWh"
              accent={hud.cyan}
            />
            <TelemetryStat
              label="CO2 Offset"
              value={rollup ? String(rollup.co2SavedKg) : "—"}
              unit="kg saved"
              accent={hud.mint}
            />
            <Box sx={{ gridColumn: { xs: "span 2", sm: "span 1" } }}>
              <TelemetryStat
                label="Trust Score"
                value={rollup ? `${rollup.trustScorePercent}%` : "—"}
                sub={rollup ? rollup.trustTier : undefined}
                accent={hud.mint}
              />
            </Box>
          </Box>
        </Box>

        {/* Tabs */}
        <Tabs
          value={tab}
          onChange={(_, v) => setTab(v)}
          variant="scrollable"
          scrollButtons="auto"
          sx={{
            mt: 2,
            borderBottom: `1px solid ${hud.border}`,
            "& .MuiTabs-indicator": { backgroundColor: hud.mint },
          }}
        >
          <Tab label="Rewards" />
          <Tab label="Sessions" />
          <Tab label="My EV" />
          <Tab label="Activity" />
        </Tabs>

        <Box sx={{ pt: 2 }}>
          {loading ? (
            <Stack alignItems="center" py={4}>
              <CircularProgress />
            </Stack>
          ) : tab === 0 ? (
            <Stack spacing={1.5}>
              {/* Incentive description */}
              <Stack
                direction="row"
                spacing={1.25}
                sx={{
                  bgcolor: "#10141d",
                  p: 1.75,
                  borderRadius: 1,
                  border: `1px solid ${hud.border}`,
                }}
              >
                <Box sx={{ color: hud.mint, mt: 0.25 }}>
                  <BoltRoundedIcon sx={{ fontSize: 18 }} />
                </Box>
                <Typography variant="body2" sx={{ color: hud.textHigh, fontSize: 12.5 }}>
                  Earn kilowatt points by verifying charging stations, reporting real-time bay
                  availability, and logging charge data. Redeem points for fast-charging vouchers
                  and technical gear.
                </Typography>
              </Stack>

              <Stack direction="row" justifyContent="space-between">
                <MicroLabel>Redemption Catalog</MicroLabel>
                <MicroLabel>Current: {balance} PTS</MicroLabel>
              </Stack>

              {prizes.length === 0 ? (
                <Typography color="text.secondary" py={2} textAlign="center">
                  No prizes available yet.
                </Typography>
              ) : (
                sortedPrizes.map((p) => {
                  const affordable = balance >= p.pointCost;
                  const pct = Math.min(100, Math.round((balance / p.pointCost) * 100));
                  const remaining = Math.max(0, p.pointCost - balance);
                  return (
                    <Box
                      key={p.id}
                      sx={{
                        p: 1.75,
                        borderRadius: 1.5,
                        bgcolor: hud.surface2,
                        border: `1px solid ${affordable ? "rgba(0,255,157,.4)" : hud.border}`,
                        transition: "border-color .2s ease, box-shadow .2s ease",
                        "&:hover": {
                          borderColor: affordable ? hud.mint : hud.textMuted,
                          boxShadow: affordable ? "0 0 18px rgba(0,255,157,.15)" : "none",
                        },
                      }}
                    >
                      <Stack direction="row" justifyContent="space-between" spacing={1.5}>
                        <Stack direction="row" spacing={1.5} sx={{ minWidth: 0 }}>
                          <Avatar
                            variant="rounded"
                            src={p.imageId ? api.imageUrl(p.imageId) : undefined}
                            sx={{
                              width: 40,
                              height: 40,
                              bgcolor: affordable ? "rgba(0,255,157,.10)" : hud.surface3,
                              color: affordable ? hud.mint : hud.textMuted,
                              border: `1px solid ${affordable ? "rgba(0,255,157,.5)" : hud.border}`,
                            }}
                          >
                            {affordable ? <EmojiEventsRoundedIcon /> : <LockRoundedIcon />}
                          </Avatar>
                          <Box sx={{ minWidth: 0 }}>
                            <Stack direction="row" spacing={1} alignItems="center">
                              <Typography variant="subtitle2" noWrap sx={{ color: hud.textHigh }}>
                                {p.name}
                              </Typography>
                              {affordable && (
                                <Chip
                                  label="Unlocked"
                                  size="small"
                                  sx={{
                                    height: 16,
                                    bgcolor: "rgba(0,255,157,.10)",
                                    color: hud.mint,
                                    border: `1px solid rgba(0,255,157,.3)`,
                                    "& .MuiChip-label": { px: 0.5, fontSize: 9 },
                                  }}
                                />
                              )}
                            </Stack>
                            {p.description && (
                              <Typography variant="caption" sx={{ color: hud.textMuted }}>
                                {p.description}
                              </Typography>
                            )}
                          </Box>
                        </Stack>
                        <Box sx={{ textAlign: "right", flexShrink: 0 }}>
                          <Typography
                            sx={{
                              fontFamily: "'JetBrains Mono', monospace",
                              fontWeight: 700,
                              fontSize: 14,
                              color: affordable ? hud.mint : hud.textHigh,
                            }}
                          >
                            {p.pointCost} pts
                          </Typography>
                          <Typography sx={{ fontSize: 10, color: hud.textMuted }}>
                            {affordable ? "ready" : `need ${remaining} pts`}
                          </Typography>
                        </Box>
                      </Stack>
                      <Stack
                        direction="row"
                        spacing={1.5}
                        alignItems="center"
                        sx={{ mt: 1.5, pt: 1.25, borderTop: `1px solid ${hud.border}` }}
                      >
                        <LinearProgress
                          variant="determinate"
                          value={pct}
                          sx={{
                            flex: 1,
                            height: 6,
                            borderRadius: 1,
                            bgcolor: "rgba(11,14,20,.9)",
                            "& .MuiLinearProgress-bar": {
                              background: affordable
                                ? `linear-gradient(90deg,${hud.cyan},${hud.mint})`
                                : hud.cyan,
                            },
                          }}
                        />
                        <Typography
                          sx={{
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: 11,
                            color: affordable ? hud.mint : hud.textMuted,
                            whiteSpace: "nowrap",
                          }}
                        >
                          {balance} / {p.pointCost} PTS
                        </Typography>
                      </Stack>
                    </Box>
                  );
                })
              )}
            </Stack>
          ) : tab === 1 ? (
            <SessionsTab sessions={sessions} ledger={ledger} onLogged={reload} />
          ) : tab === 2 ? (
            <VehiclePanel />
          ) : (
            <ActivitiesPanel />
          )}
        </Box>
      </Box>
    </Dialog>
  );
}
