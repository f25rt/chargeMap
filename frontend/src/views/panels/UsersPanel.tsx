import { useEffect, useState } from "react";
import {
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  Divider,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import BlockRoundedIcon from "@mui/icons-material/BlockRounded";
import CheckCircleRoundedIcon from "@mui/icons-material/CheckCircleRounded";
import StarRoundedIcon from "@mui/icons-material/StarRounded";
import { api } from "../../api/client";
import type { ManagedUser } from "../../api/types";

/** User management table: suspend / unsuspend, shared by admin + operator. */
export default function UsersPanel() {
  const [users, setUsers] = useState<ManagedUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [reasonFor, setReasonFor] = useState<string | null>(null);
  const [reason, setReason] = useState("");
  const [adjustFor, setAdjustFor] = useState<string | null>(null);
  const [adjustDelta, setAdjustDelta] = useState("");
  const [adjustReason, setAdjustReason] = useState("");

  const load = async () => {
    setLoading(true);
    try {
      setUsers(await api.moderationUsers());
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const patch = (u: ManagedUser) =>
    setUsers((prev) => prev.map((x) => (x.id === u.id ? u : x)));

  const suspend = async (id: string) => {
    if (!reason.trim()) return;
    setBusyId(id);
    try {
      patch(await api.moderationSuspend(id, reason.trim()));
      setReasonFor(null);
      setReason("");
    } finally {
      setBusyId(null);
    }
  };

  const unsuspend = async (id: string) => {
    setBusyId(id);
    try {
      patch(await api.moderationUnsuspend(id));
    } finally {
      setBusyId(null);
    }
  };

  const adjust = async (id: string) => {
    if (adjustDelta === "" || !adjustReason.trim()) return;
    setBusyId(id);
    try {
      await api.adjustPoints(id, Number(adjustDelta), adjustReason.trim());
      setAdjustFor(null);
      setAdjustDelta("");
      setAdjustReason("");
      await load();
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
    <Card sx={{ p: 2 }}>
      <Typography variant="subtitle1" gutterBottom>
        Users
      </Typography>
      <Divider sx={{ mb: 1 }} />
      <Stack divider={<Divider />}>
        {users.map((u) => (
          <Box key={u.id} sx={{ py: 1.25 }}>
            <Stack
              direction="row"
              alignItems="center"
              justifyContent="space-between"
              spacing={1}
            >
              <Box sx={{ minWidth: 0 }}>
                <Stack direction="row" spacing={0.75} alignItems="center">
                  <Typography variant="body2" fontWeight={600} noWrap>
                    {u.name}
                  </Typography>
                  <Chip label={u.role} size="small" variant="outlined" />
                  {u.suspended && <Chip label="Suspended" size="small" color="error" />}
                </Stack>
                <Typography variant="caption" color="text.secondary">
                  {u.email} · {u.level} · {u.pointsBalance} pts · {u.stationsAdded} added
                </Typography>
              </Box>
              <Stack direction="row" spacing={0.5}>
                <Button
                  size="small"
                  startIcon={<StarRoundedIcon />}
                  disabled={busyId === u.id}
                  onClick={() => setAdjustFor(adjustFor === u.id ? null : u.id)}
                >
                  Points
                </Button>
                {u.suspended ? (
                  <Button
                    size="small"
                    color="success"
                    startIcon={<CheckCircleRoundedIcon />}
                    disabled={busyId === u.id}
                    onClick={() => unsuspend(u.id)}
                  >
                    Unsuspend
                  </Button>
                ) : (
                  <Button
                    size="small"
                    color="error"
                    startIcon={<BlockRoundedIcon />}
                    disabled={busyId === u.id || u.role === "ADMIN"}
                    onClick={() => setReasonFor(reasonFor === u.id ? null : u.id)}
                  >
                    Suspend
                  </Button>
                )}
              </Stack>
            </Stack>

            {adjustFor === u.id && (
              <Stack direction="row" spacing={1} mt={1}>
                <TextField
                  size="small"
                  type="number"
                  label="± points"
                  value={adjustDelta}
                  onChange={(e) => setAdjustDelta(e.target.value)}
                  sx={{ width: 110 }}
                />
                <TextField
                  size="small"
                  fullWidth
                  label="Reason"
                  value={adjustReason}
                  onChange={(e) => setAdjustReason(e.target.value)}
                />
                <Button
                  variant="contained"
                  disabled={adjustDelta === "" || !adjustReason.trim() || busyId === u.id}
                  onClick={() => adjust(u.id)}
                >
                  Apply
                </Button>
              </Stack>
            )}

            {reasonFor === u.id && (
              <Stack direction="row" spacing={1} mt={1}>
                <TextField
                  size="small"
                  fullWidth
                  label="Reason"
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                />
                <Button
                  variant="contained"
                  color="error"
                  disabled={!reason.trim() || busyId === u.id}
                  onClick={() => suspend(u.id)}
                >
                  Confirm
                </Button>
              </Stack>
            )}
          </Box>
        ))}
      </Stack>
    </Card>
  );
}
