import { useEffect, useRef, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Dialog,
  IconButton,
  LinearProgress,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import CloseRoundedIcon from "@mui/icons-material/CloseRounded";
import PhotoCameraRoundedIcon from "@mui/icons-material/PhotoCameraRounded";
import MyLocationRoundedIcon from "@mui/icons-material/MyLocationRounded";
import { api } from "../api/client";
import { analyzeImage } from "../ocr/ocr";

interface Props {
  open: boolean;
  onClose: () => void;
  onSubmitted: () => void;
  defaultCenter: [number, number];
  /** Location pinned on the map before opening; pre-fills lat/lng. */
  initialLocation?: [number, number] | null;
}

const CONNECTORS = ["", "CCS1", "CCS2", "CHADEMO", "TYPE2", "GBT", "NACS"];
const CHARGER_TYPES = ["", "AC", "DC", "DC_FAST"];

export default function AddStationDialog({
  open,
  onClose,
  onSubmitted,
  defaultCenter,
  initialLocation,
}: Props) {
  const fileRef = useRef<HTMLInputElement>(null);
  const [image, setImage] = useState<File | null>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [ocrPct, setOcrPct] = useState<number | null>(null);
  const [ocrText, setOcrText] = useState("");
  const [looksLikeStation, setLooksLikeStation] = useState<boolean | null>(null);

  const [name, setName] = useState("");
  const [address, setAddress] = useState("");
  const [lat, setLat] = useState(String(defaultCenter[0]));
  const [lng, setLng] = useState(String(defaultCenter[1]));
  const [price, setPrice] = useState("");
  const [connector, setConnector] = useState("");
  const [chargerType, setChargerType] = useState("");
  const [powerKw, setPowerKw] = useState("");

  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // When opened with a pinned location, pre-fill the coordinates.
  useEffect(() => {
    if (open && initialLocation) {
      setLat(String(initialLocation[0]));
      setLng(String(initialLocation[1]));
    }
  }, [open, initialLocation]);

  const reset = () => {
    setImage(null);
    setPreview(null);
    setOcrPct(null);
    setOcrText("");
    setLooksLikeStation(null);
    setName("");
    setAddress("");
    setPrice("");
    setConnector("");
    setChargerType("");
    setPowerKw("");
    setError(null);
  };

  const pickImage = async (file: File) => {
    setImage(file);
    setPreview(URL.createObjectURL(file));
    setError(null);
    setOcrPct(0);
    try {
      const res = await analyzeImage(file, setOcrPct);
      setOcrText(res.text);
      setLooksLikeStation(res.looksLikeStation);
      if (res.detectedPrice != null) setPrice(String(res.detectedPrice));
    } catch {
      setError("Could not read the image text. You can still fill details manually.");
    } finally {
      setOcrPct(null);
    }
  };

  const useMyLocation = () => {
    if (!navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setLat(String(pos.coords.latitude));
        setLng(String(pos.coords.longitude));
      },
      () => setError("Could not get your location; enter it manually."),
    );
  };

  const submit = async () => {
    if (!image) {
      setError("Please add a photo of the station.");
      return;
    }
    if (!name.trim()) {
      setError("Please enter a station name.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await api.createSubmission({
        image,
        name: name.trim(),
        address: address.trim() || undefined,
        area: address.trim() || undefined,
        lat: Number(lat),
        lng: Number(lng),
        ocrText,
        ocrLooksLikeStation: !!looksLikeStation,
        pricePerKwh: price === "" ? null : Number(price),
        connectorType: connector as never,
        chargerType: chargerType as never,
        powerKw: powerKw === "" ? null : Number(powerKw),
      });
      reset();
      onSubmitted();
      onClose();
    } catch (e: unknown) {
      const status = (e as { response?: { status?: number } })?.response?.status;
      if (status === 403) setError("Your account cannot submit stations right now.");
      else if (status === 401) setError("Please sign in to submit a station.");
      else setError("Submission failed. Please try again.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <Box sx={{ px: 3, pt: 2.5, pb: 1, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <Typography variant="h6">Add a charging station</Typography>
        <IconButton onClick={onClose} size="small" sx={{ bgcolor: "#f3f4f6" }}>
          <CloseRoundedIcon fontSize="small" />
        </IconButton>
      </Box>

      <Box sx={{ px: 3, pb: 3 }}>
        <Stack spacing={2}>
          {error && <Alert severity="error">{error}</Alert>}

          {/* Image + OCR */}
          <input
            ref={fileRef}
            type="file"
            accept="image/*"
            hidden
            onChange={(e) => e.target.files?.[0] && pickImage(e.target.files[0])}
          />
          {preview ? (
            <Box sx={{ position: "relative" }}>
              <Box
                component="img"
                src={preview}
                sx={{ width: "100%", maxHeight: 200, objectFit: "cover", borderRadius: 2 }}
              />
              <Button size="small" sx={{ mt: 1 }} onClick={() => fileRef.current?.click()}>
                Change photo
              </Button>
            </Box>
          ) : (
            <Button
              variant="outlined"
              startIcon={<PhotoCameraRoundedIcon />}
              onClick={() => fileRef.current?.click()}
              sx={{ py: 2 }}
            >
              Add station photo
            </Button>
          )}

          {ocrPct != null && (
            <Box>
              <Typography variant="caption" color="text.secondary">
                Reading photo… {ocrPct}%
              </Typography>
              <LinearProgress variant="determinate" value={ocrPct} sx={{ borderRadius: 2 }} />
            </Box>
          )}
          {looksLikeStation === false && (
            <Alert severity="warning">
              This photo doesn't look like a charging station. You can still submit, but a
              reviewer will double-check.
            </Alert>
          )}
          {looksLikeStation === true && (
            <Alert severity="success">Looks like a charging station — nice.</Alert>
          )}

          <TextField label="Station name" value={name} onChange={(e) => setName(e.target.value)} fullWidth />
          <TextField
            label="Address / area"
            value={address}
            onChange={(e) => setAddress(e.target.value)}
            fullWidth
          />

          <Stack direction="row" spacing={1}>
            <TextField label="Latitude" value={lat} onChange={(e) => setLat(e.target.value)} fullWidth />
            <TextField label="Longitude" value={lng} onChange={(e) => setLng(e.target.value)} fullWidth />
            <IconButton onClick={useMyLocation} title="Use my location" sx={{ bgcolor: "#f3f4f6" }}>
              <MyLocationRoundedIcon />
            </IconButton>
          </Stack>

          <TextField
            label="Price ₱/kWh (auto-filled from photo if detected)"
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            type="number"
            fullWidth
          />

          <Stack direction="row" spacing={1}>
            <TextField
              select
              label="Connector"
              value={connector}
              onChange={(e) => setConnector(e.target.value)}
              InputLabelProps={{ shrink: true }}
              fullWidth
            >
              {CONNECTORS.map((c) => (
                <MenuItem key={c} value={c}>
                  {c || "—"}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              select
              label="Type"
              value={chargerType}
              onChange={(e) => setChargerType(e.target.value)}
              InputLabelProps={{ shrink: true }}
              fullWidth
            >
              {CHARGER_TYPES.map((c) => (
                <MenuItem key={c} value={c}>
                  {c ? c.replace("_", " ") : "—"}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="kW"
              value={powerKw}
              onChange={(e) => setPowerKw(e.target.value)}
              type="number"
              sx={{ width: 90 }}
            />
          </Stack>

          <Button variant="contained" size="large" onClick={submit} disabled={busy}>
            {busy ? "Submitting…" : "Submit for review"}
          </Button>
          <Typography variant="caption" color="text.secondary" textAlign="center">
            You'll earn points when a reviewer approves your submission.
          </Typography>
        </Stack>
      </Box>
    </Dialog>
  );
}
