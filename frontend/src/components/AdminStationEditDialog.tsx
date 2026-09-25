import { useEffect, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { api } from "../api/client";
import type { StationDetail } from "../api/types";

interface Props {
  station: StationDetail | null;
  onClose: () => void;
  /** Called with the updated station after a successful save. */
  onSaved: (updated: StationDetail) => void;
}

/**
 * Admin/operator inline editor for a station reached by clicking its map pin. Edits apply
 * immediately (admins don't need approval) to name/operator/address/area, pricing, and
 * coordinates.
 */
export default function AdminStationEditDialog({ station, onClose, onSaved }: Props) {
  const [name, setName] = useState("");
  const [operator, setOperator] = useState("");
  const [address, setAddress] = useState("");
  const [area, setArea] = useState("");
  const [price, setPrice] = useState("");
  const [lat, setLat] = useState("");
  const [lng, setLng] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!station) return;
    setName(station.name ?? "");
    setOperator(station.operator ?? "");
    setAddress(station.address ?? "");
    setArea(station.area ?? "");
    setPrice(station.currentPricing?.pricePerKwh != null ? String(station.currentPricing.pricePerKwh) : "");
    setLat(station.location?.lat != null ? String(station.location.lat) : "");
    setLng(station.location?.lng != null ? String(station.location.lng) : "");
    setError(null);
  }, [station]);

  const save = async () => {
    if (!station) return;
    setSaving(true);
    setError(null);
    try {
      let updated = await api.adminUpdateStationDetails(station.id, {
        name: name.trim(),
        operator: operator.trim(),
        address: address.trim(),
        area: area.trim(),
      });

      // Location, if changed and valid.
      const latNum = lat === "" ? null : Number(lat);
      const lngNum = lng === "" ? null : Number(lng);
      if (
        latNum != null &&
        lngNum != null &&
        (latNum !== station.location?.lat || lngNum !== station.location?.lng)
      ) {
        updated = await api.adminUpdateStationLocation(station.id, latNum, lngNum);
      }

      // Pricing, if changed. Reuses the operator pricing endpoint.
      const priceNum = price === "" ? null : Number(price);
      const currentPrice = station.currentPricing?.pricePerKwh ?? null;
      if (priceNum != null && priceNum !== currentPrice) {
        updated = await api.operatorUpdatePricing(station.id, priceNum);
      }

      onSaved(updated);
      onClose();
    } catch {
      setError("Could not save changes. You may lack permission for one of these fields.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={!!station} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Edit station</DialogTitle>
      <DialogContent dividers>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        <Stack spacing={2} sx={{ mt: 0.5 }}>
          <TextField label="Name" value={name} onChange={(e) => setName(e.target.value)} fullWidth size="small" />
          <TextField
            label="Operator"
            value={operator}
            onChange={(e) => setOperator(e.target.value)}
            fullWidth
            size="small"
          />
          <TextField
            label="Address"
            value={address}
            onChange={(e) => setAddress(e.target.value)}
            fullWidth
            size="small"
          />
          <TextField label="Area" value={area} onChange={(e) => setArea(e.target.value)} fullWidth size="small" />
          <TextField
            label="Price (₱/kWh)"
            type="number"
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            fullWidth
            size="small"
          />
          <Box>
            <Typography variant="caption" color="text.secondary">
              Coordinates
            </Typography>
            <Stack direction={{ xs: "column", sm: "row" }} spacing={2} sx={{ mt: 0.5 }}>
              <TextField
                label="Latitude"
                type="number"
                value={lat}
                onChange={(e) => setLat(e.target.value)}
                fullWidth
                size="small"
              />
              <TextField
                label="Longitude"
                type="number"
                value={lng}
                onChange={(e) => setLng(e.target.value)}
                fullWidth
                size="small"
              />
            </Stack>
          </Box>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={saving}>
          Cancel
        </Button>
        <Button variant="contained" onClick={save} disabled={saving}>
          {saving ? "Saving…" : "Save changes"}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
