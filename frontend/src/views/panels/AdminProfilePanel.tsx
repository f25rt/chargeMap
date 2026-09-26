import { useEffect, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Card,
  Divider,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { api } from "../../api/client";
import type { Branch } from "../../api/types";
import { useAuth } from "../../auth/AuthContext";

/** Admin self-profile: edit name, view assigned branch, change password. */
export default function AdminProfilePanel() {
  const { user, refresh } = useAuth();
  const [name, setName] = useState(user?.name ?? "");
  const [branchName, setBranchName] = useState<string>("—");
  const [savingName, setSavingName] = useState(false);
  const [nameMsg, setNameMsg] = useState<string | null>(null);

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [savingPw, setSavingPw] = useState(false);
  const [pwMsg, setPwMsg] = useState<string | null>(null);
  const [pwErr, setPwErr] = useState<string | null>(null);

  useEffect(() => {
    setName(user?.name ?? "");
    if (user?.branchId) {
      api
        .branches()
        .then((bs) => setBranchName(bs.find((b: Branch) => b.id === user.branchId)?.name ?? "(unknown)"))
        .catch(() => setBranchName("(unknown)"));
    } else {
      setBranchName("Unassigned");
    }
  }, [user]);

  const saveName = async () => {
    if (!name.trim()) return;
    setSavingName(true);
    setNameMsg(null);
    try {
      await api.updateName(name.trim());
      await refresh();
      setNameMsg("Name updated.");
    } finally {
      setSavingName(false);
    }
  };

  const changePassword = async () => {
    setPwMsg(null);
    setPwErr(null);
    if (newPassword.length < 8) {
      setPwErr("New password must be at least 8 characters.");
      return;
    }
    setSavingPw(true);
    try {
      await api.changePassword(currentPassword, newPassword);
      setCurrentPassword("");
      setNewPassword("");
      setPwMsg("Password changed.");
    } catch {
      setPwErr("Could not change password. Check your current password.");
    } finally {
      setSavingPw(false);
    }
  };

  return (
    <Box
      sx={{
        display: "grid",
        gridTemplateColumns: { xs: "1fr", md: "1fr 1fr" },
        gap: 3,
        alignItems: "start",
      }}
    >
      <Card sx={{ p: 2 }}>
        <Typography variant="subtitle1" gutterBottom>
          Profile
        </Typography>
        <Divider sx={{ mb: 2 }} />
        <Stack spacing={2}>
          <TextField size="small" label="Email" value={user?.email ?? ""} fullWidth disabled />
          <TextField size="small" label="Role" value={user?.role ?? ""} fullWidth disabled />
          <TextField size="small" label="Assigned branch" value={branchName} fullWidth disabled />
          <TextField
            size="small"
            label="Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            fullWidth
          />
          {nameMsg && <Alert severity="success">{nameMsg}</Alert>}
          <Button variant="contained" onClick={saveName} disabled={savingName || !name.trim()}>
            Save name
          </Button>
        </Stack>
      </Card>

      <Card sx={{ p: 2 }}>
        <Typography variant="subtitle1" gutterBottom>
          Change password
        </Typography>
        <Divider sx={{ mb: 2 }} />
        <Stack spacing={2}>
          <TextField
            size="small"
            type="password"
            label="Current password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            fullWidth
            autoComplete="current-password"
          />
          <TextField
            size="small"
            type="password"
            label="New password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            helperText="At least 8 characters"
            fullWidth
            autoComplete="new-password"
          />
          {pwMsg && <Alert severity="success">{pwMsg}</Alert>}
          {pwErr && <Alert severity="error">{pwErr}</Alert>}
          <Button
            variant="contained"
            onClick={changePassword}
            disabled={savingPw || !currentPassword || !newPassword}
          >
            Change password
          </Button>
        </Stack>
      </Card>
    </Box>
  );
}
