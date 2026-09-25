import { useState } from "react";
import {
  Alert,
  Box,
  Button,
  Dialog,
  IconButton,
  Slide,
  Stack,
  Tab,
  Tabs,
  TextField,
  Typography,
  useMediaQuery,
} from "@mui/material";
import type { TransitionProps } from "@mui/material/transitions";
import CloseRoundedIcon from "@mui/icons-material/CloseRounded";
import BoltRoundedIcon from "@mui/icons-material/BoltRounded";
import { forwardRef } from "react";
import { useAuth } from "../auth/AuthContext";
import { useTheme } from "@mui/material/styles";
import VehiclePanel from "./VehiclePanel";

interface Props {
  open: boolean;
  onClose: () => void;
}

// Slide up from the bottom on mobile (native sheet feel).
const SlideUp = forwardRef(function SlideUp(
  props: TransitionProps & { children: React.ReactElement },
  ref: React.Ref<unknown>,
) {
  return <Slide direction="up" ref={ref} {...props} />;
});

export default function AuthDialog({ open, onClose }: Props) {
  const { login, register } = useAuth();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down("sm"));

  const [tab, setTab] = useState(0);
  const [email, setEmail] = useState("");
  const [name, setName] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  // After a successful registration we show an optional "connect your vehicle" step.
  const [vehicleStep, setVehicleStep] = useState(false);

  const close = () => {
    setVehicleStep(false);
    onClose();
  };

  const submit = async () => {
    setBusy(true);
    setError(null);
    try {
      if (tab === 0) {
        await login(email, password);
        onClose();
      } else {
        await register(email, name, password);
        // Registered + auto-logged-in: offer the optional vehicle sync step.
        setVehicleStep(true);
      }
    } catch (e: unknown) {
      const status = (e as { response?: { status?: number } })?.response?.status;
      if (status === 409) setError("That email is already registered.");
      else if (status === 401) setError("Invalid email or password.");
      else setError("Something went wrong. Check the details and try again.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog
      open={open}
      onClose={close}
      fullWidth
      maxWidth="xs"
      TransitionComponent={isMobile ? SlideUp : undefined}
      // Center on desktop; dock to the bottom as a sheet on mobile.
      sx={{
        "& .MuiDialog-container": {
          alignItems: isMobile ? "flex-end" : "center",
          justifyContent: "center",
        },
      }}
      PaperProps={{
        sx: {
          m: isMobile ? 0 : 2,
          width: "100%",
          borderRadius: isMobile ? "20px 20px 0 0" : 3,
          pb: isMobile ? "var(--safe-bottom)" : 0,
        },
      }}
    >
      {/* Header */}
      <Box sx={{ px: 3, pt: 2.5, pb: 1 }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <Stack direction="row" spacing={1} alignItems="center">
            <Box
              sx={{
                width: 34,
                height: 34,
                borderRadius: "50%",
                bgcolor: "primary.main",
                color: "#fff",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
              }}
            >
              <BoltRoundedIcon fontSize="small" />
            </Box>
            <Typography variant="h6">{vehicleStep ? "Connect your EV" : "Welcome"}</Typography>
          </Stack>
          <IconButton onClick={close} size="small" sx={{ bgcolor: "#f3f4f6" }}>
            <CloseRoundedIcon fontSize="small" />
          </IconButton>
        </Stack>
      </Box>

      {vehicleStep ? (
        <Box sx={{ px: 3, py: 2.5 }}>
          <Typography variant="body2" color="text.secondary" mb={2}>
            Your account is ready. Optionally connect your electric vehicle now to see live
            battery, range, and location — or skip and do it later from your profile.
          </Typography>
          <VehiclePanel onConnected={close} />
          <Button fullWidth sx={{ mt: 2 }} onClick={close}>
            Skip for now
          </Button>
        </Box>
      ) : (
        <>

      <Tabs
        value={tab}
        onChange={(_, v) => setTab(v)}
        variant="fullWidth"
        sx={{ px: 2, borderBottom: "1px solid", borderColor: "divider" }}
      >
        <Tab label="Sign in" />
        <Tab label="Register" />
      </Tabs>

      <Box sx={{ px: 3, py: 2.5 }}>
        <Stack spacing={2}>
          {error && <Alert severity="error">{error}</Alert>}
          <TextField
            id="auth-email"
            label="Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            fullWidth
            autoComplete="email"
          />
          {tab === 1 && (
            <TextField
              id="auth-name"
              label="Name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              fullWidth
              autoComplete="name"
            />
          )}
          <TextField
            id="auth-password"
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && submit()}
            helperText={tab === 1 ? "At least 8 characters" : undefined}
            fullWidth
            autoComplete={tab === 0 ? "current-password" : "new-password"}
          />
          <Button variant="contained" size="large" onClick={submit} disabled={busy}>
            {tab === 0 ? "Sign in" : "Create account"}
          </Button>
        </Stack>
      </Box>
      </>
      )}
    </Dialog>
  );
}
