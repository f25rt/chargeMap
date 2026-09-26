import { useEffect, useState } from "react";
import {
  Avatar,
  Box,
  Card,
  Chip,
  CircularProgress,
  Dialog,
  Divider,
  IconButton,
  Stack,
  Tab,
  Tabs,
  Typography,
} from "@mui/material";
import CloseRoundedIcon from "@mui/icons-material/CloseRounded";
import EmojiEventsRoundedIcon from "@mui/icons-material/EmojiEventsRounded";
import StarRoundedIcon from "@mui/icons-material/StarRounded";
import LogoutRoundedIcon from "@mui/icons-material/LogoutRounded";
import { Button } from "@mui/material";
import VehiclePanel from "./VehiclePanel";
import ActivitiesPanel from "./ActivitiesPanel";
import { api } from "../api/client";
import type { PointsLedgerEntry, Prize } from "../api/types";
import { useAuth } from "../auth/AuthContext";

interface Props {
  open: boolean;
  onClose: () => void;
}

const TASK_LABEL: Record<string, string> = {
  STATION_ADD: "Added a station",
  STATION_UPDATE: "Updated a station",
  REPORT: "Reported status",
  ADMIN_ADJUST: "Adjustment",
};

export default function ProfileSheet({ open, onClose }: Props) {
  const { user, refresh, logout } = useAuth();
  const [tab, setTab] = useState(0);
  const [ledger, setLedger] = useState<PointsLedgerEntry[]>([]);
  const [prizes, setPrizes] = useState<Prize[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!open) return;
    setLoading(true);
    Promise.all([refresh(), api.myPoints(), api.prizes()])
      .then(([, l, p]) => {
        setLedger(l);
        setPrizes(p);
      })
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const balance = user?.pointsBalance ?? 0;

  return (
    <Dialog
      open={open}
      onClose={onClose}
      fullWidth
      maxWidth="xs"
      scroll="paper"
      PaperProps={{
        sx: {
          borderRadius: 3,
          m: 2,
          width: "100%",
          maxWidth: 480,
          maxHeight: "calc(100dvh - 32px)",
          display: "flex",
          flexDirection: "column",
        },
      }}
    >
      <Box sx={{ px: 2.5, pt: 2 }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <Typography variant="h6">Your profile</Typography>
          <Stack direction="row" spacing={1}>
            <Button
              size="small"
              color="inherit"
              startIcon={<LogoutRoundedIcon />}
              onClick={() => {
                logout();
                onClose();
              }}
            >
              Sign out
            </Button>
            <IconButton onClick={onClose} size="small" sx={{ bgcolor: "#f3f4f6" }}>
              <CloseRoundedIcon fontSize="small" />
            </IconButton>
          </Stack>
        </Stack>

        {/* Points hero */}
        <Card
          sx={{
            mt: 1.5,
            p: 2,
            color: "#fff",
            background: "linear-gradient(135deg,#1f9d57,#14713d)",
          }}
        >
          <Stack direction="row" spacing={2} alignItems="center">
            <Avatar sx={{ bgcolor: "rgba(255,255,255,.25)", width: 52, height: 52 }}>
              {user?.name.charAt(0).toUpperCase()}
            </Avatar>
            <Box sx={{ flex: 1 }}>
              <Typography variant="subtitle1">{user?.name}</Typography>
              <Stack direction="row" spacing={0.5} alignItems="center">
                <StarRoundedIcon sx={{ fontSize: 16 }} />
                <Typography variant="body2">{user?.level}</Typography>
              </Stack>
            </Box>
            <Box sx={{ textAlign: "right" }}>
              <Typography variant="h4" fontWeight={800} lineHeight={1}>
                {balance}
              </Typography>
              <Typography variant="caption">points</Typography>
            </Box>
          </Stack>
          <Stack direction="row" spacing={2} mt={1.5}>
            <Typography variant="caption">🔌 {user?.stationsAdded ?? 0} stations added</Typography>
            <Typography variant="caption">✏️ {user?.updatesMade ?? 0} updates</Typography>
          </Stack>
        </Card>

        <Tabs
          value={tab}
          onChange={(_, v) => setTab(v)}
          variant="scrollable"
          scrollButtons="auto"
          sx={{ mt: 1 }}
        >
          <Tab label="Rewards" />
          <Tab label="History" />
          <Tab label="Vehicle" />
          <Tab label="Activities" />
        </Tabs>
      </Box>

      <Box sx={{ px: 2.5, py: 2, overflowY: "auto", flex: 1, minHeight: 0 }}>
        {loading ? (
          <Stack alignItems="center" py={4}>
            <CircularProgress />
          </Stack>
        ) : tab === 0 ? (
          <Stack spacing={1.5}>
            <Typography variant="body2" color="text.secondary">
              Earn points by adding and updating stations. Redeeming prizes is coming soon.
            </Typography>
            {prizes.length === 0 ? (
              <Typography color="text.secondary" py={2} textAlign="center">
                No prizes available yet.
              </Typography>
            ) : (
              prizes.map((p) => {
                const affordable = balance >= p.pointCost;
                return (
                  <Card key={p.id} sx={{ p: 1.5, display: "flex", gap: 1.5, alignItems: "center" }}>
                    <Avatar
                      variant="rounded"
                      src={p.imageId ? api.imageUrl(p.imageId) : undefined}
                      sx={{ bgcolor: "rgba(31,157,87,.12)", color: "primary.main", width: 48, height: 48 }}
                    >
                      <EmojiEventsRoundedIcon />
                    </Avatar>
                    <Box sx={{ flex: 1, minWidth: 0 }}>
                      <Typography variant="subtitle2" noWrap>
                        {p.name}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {p.description}
                      </Typography>
                    </Box>
                    <Chip
                      label={`${p.pointCost} pts`}
                      color={affordable ? "success" : "default"}
                      size="small"
                    />
                  </Card>
                );
              })
            )}
          </Stack>
        ) : tab === 1 ? (
          <Stack divider={<Divider />}>
            {ledger.length === 0 ? (
              <Typography color="text.secondary" py={2} textAlign="center">
                No activity yet — add your first station to earn points.
              </Typography>
            ) : (
              ledger.map((e, i) => (
                <Stack
                  key={i}
                  direction="row"
                  justifyContent="space-between"
                  alignItems="center"
                  sx={{ py: 1 }}
                >
                  <Box>
                    <Typography variant="body2" fontWeight={600}>
                      {TASK_LABEL[e.taskType] ?? e.taskType}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {e.reason ?? new Date(e.createdAt).toLocaleString()}
                    </Typography>
                  </Box>
                  <Typography
                    variant="body2"
                    fontWeight={700}
                    color={e.points >= 0 ? "success.main" : "error.main"}
                  >
                    {e.points >= 0 ? "+" : ""}
                    {e.points}
                  </Typography>
                </Stack>
              ))
            )}
          </Stack>
        ) : tab === 2 ? (
          <VehiclePanel />
        ) : (
          <ActivitiesPanel />
        )}
      </Box>
    </Dialog>
  );
}
