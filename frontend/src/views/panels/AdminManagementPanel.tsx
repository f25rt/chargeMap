import { useEffect, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  Divider,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import BlockRoundedIcon from "@mui/icons-material/BlockRounded";
import CheckCircleRoundedIcon from "@mui/icons-material/CheckCircleRounded";
import PersonAddRoundedIcon from "@mui/icons-material/PersonAddRounded";
import { api } from "../../api/client";
import type { AdminSummary, Branch } from "../../api/types";

/** Super-admin oversight of admins: create, reassign branch, disable/enable. */
export default function AdminManagementPanel() {
  const [admins, setAdmins] = useState<AdminSummary[]>([]);
  const [branches, setBranches] = useState<Branch[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<string | null>(null);

  // Create-admin form
  const [newEmail, setNewEmail] = useState("");
  const [newName, setNewName] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newBranch, setNewBranch] = useState("");
  const [creating, setCreating] = useState(false);
  const [createMsg, setCreateMsg] = useState<string | null>(null);
  const [createErr, setCreateErr] = useState<string | null>(null);

  const load = async () => {
    setLoading(true);
    try {
      const [a, b] = await Promise.all([api.admins(), api.branches()]);
      setAdmins(a);
      setBranches(b);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const createAdmin = async () => {
    setCreateMsg(null);
    setCreateErr(null);
    if (!newEmail.trim() || !newName.trim() || newPassword.length < 8) {
      setCreateErr("Email, name, and a password of at least 8 characters are required.");
      return;
    }
    setCreating(true);
    try {
      await api.createAdmin({
        email: newEmail.trim(),
        name: newName.trim(),
        password: newPassword,
        branchId: newBranch || undefined,
      });
      setNewEmail("");
      setNewName("");
      setNewPassword("");
      setNewBranch("");
      setCreateMsg("Admin account created.");
      await load();
    } catch (e: unknown) {
      const status = (e as { response?: { status?: number } })?.response?.status;
      setCreateErr(status === 409 ? "That email is already registered." : "Could not create admin.");
    } finally {
      setCreating(false);
    }
  };

  const branchName = (id: string | null) =>
    id ? branches.find((b) => b.id === id)?.name ?? "(unknown)" : "Unassigned";

  const reassign = async (adminId: string, branchId: string) => {
    setBusyId(adminId);
    try {
      const updated = await api.reassignAdminBranch(adminId, branchId || null);
      setAdmins((prev) => prev.map((a) => (a.id === updated.id ? updated : a)));
    } finally {
      setBusyId(null);
    }
  };

  const toggleDisabled = async (a: AdminSummary) => {
    setBusyId(a.id);
    try {
      const updated = a.adminDisabled ? await api.enableAdmin(a.id) : await api.disableAdmin(a.id);
      setAdmins((prev) => prev.map((x) => (x.id === updated.id ? updated : x)));
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
      {/* Create admin */}
      <Card sx={{ p: 2 }}>
        <Stack direction="row" spacing={1} alignItems="center" mb={1}>
          <PersonAddRoundedIcon color="primary" />
          <Typography variant="subtitle1">Create admin</Typography>
        </Stack>
        <Divider sx={{ mb: 2 }} />
        {createMsg && <Alert severity="success" sx={{ mb: 1.5 }}>{createMsg}</Alert>}
        {createErr && <Alert severity="error" sx={{ mb: 1.5 }}>{createErr}</Alert>}
        <Stack spacing={1.5}>
          <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5}>
            <TextField
              size="small"
              label="Name"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              fullWidth
            />
            <TextField
              size="small"
              label="Email"
              type="email"
              value={newEmail}
              onChange={(e) => setNewEmail(e.target.value)}
              fullWidth
            />
          </Stack>
          <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5}>
            <TextField
              size="small"
              label="Password"
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              helperText="At least 8 characters"
              fullWidth
            />
            <TextField
              select
              size="small"
              label="Branch (optional)"
              value={newBranch}
              onChange={(e) => setNewBranch(e.target.value)}
              fullWidth
            >
              <MenuItem value="">Unassigned</MenuItem>
              {branches.map((b) => (
                <MenuItem key={b.id} value={b.id}>
                  {b.name}
                </MenuItem>
              ))}
            </TextField>
          </Stack>
          <Button
            variant="contained"
            startIcon={<PersonAddRoundedIcon />}
            onClick={createAdmin}
            disabled={creating}
            sx={{ alignSelf: "flex-start" }}
          >
            {creating ? "Creating…" : "Create admin"}
          </Button>
        </Stack>
      </Card>

      {/* Admins list */}
      <Card sx={{ p: 2 }}>
        <Typography variant="subtitle1" gutterBottom>
          Admins
        </Typography>
        <Divider sx={{ mb: 1 }} />
      <Stack divider={<Divider />}>
        {admins.map((a) => (
          <Stack
            key={a.id}
            direction={{ xs: "column", md: "row" }}
            alignItems={{ xs: "stretch", md: "center" }}
            justifyContent="space-between"
            spacing={1}
            sx={{ py: 1.25 }}
          >
            <Box sx={{ minWidth: 0 }}>
              <Stack direction="row" spacing={0.75} alignItems="center" flexWrap="wrap" useFlexGap>
                <Typography variant="body2" fontWeight={600} noWrap>
                  {a.name}
                </Typography>
                <Chip label={a.role} size="small" variant="outlined" />
                {a.adminDisabled && <Chip label="Disabled" size="small" color="error" />}
              </Stack>
              <Typography variant="caption" color="text.secondary">
                {a.email} · Branch: {branchName(a.branchId)}
              </Typography>
            </Box>
            <Stack direction="row" spacing={1} alignItems="center" sx={{ flexShrink: 0 }}>
              <TextField
                select
                size="small"
                label="Branch"
                value={a.branchId ?? ""}
                onChange={(e) => reassign(a.id, e.target.value)}
                disabled={busyId === a.id || a.role === "SUPER_ADMIN"}
                sx={{ minWidth: 160 }}
              >
                <MenuItem value="">Unassigned</MenuItem>
                {branches.map((b) => (
                  <MenuItem key={b.id} value={b.id}>
                    {b.name}
                  </MenuItem>
                ))}
              </TextField>
              {a.role !== "SUPER_ADMIN" &&
                (a.adminDisabled ? (
                  <Button
                    size="small"
                    color="success"
                    startIcon={<CheckCircleRoundedIcon />}
                    disabled={busyId === a.id}
                    onClick={() => toggleDisabled(a)}
                  >
                    Enable
                  </Button>
                ) : (
                  <Button
                    size="small"
                    color="error"
                    startIcon={<BlockRoundedIcon />}
                    disabled={busyId === a.id}
                    onClick={() => toggleDisabled(a)}
                  >
                    Disable
                  </Button>
                ))}
            </Stack>
          </Stack>
        ))}
      </Stack>
      <Alert severity="info" sx={{ mt: 2 }}>
        Assign admins to branches and disable accounts as needed. Super admins can't be disabled.
      </Alert>
      </Card>
    </Stack>
  );
}
