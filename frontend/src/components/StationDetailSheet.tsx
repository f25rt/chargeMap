import { useEffect, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Collapse,
  Divider,
  Drawer,
  IconButton,
  MenuItem,
  Stack,
  TextField,
  Tooltip,
  Typography,
  useMediaQuery,
} from "@mui/material";
import { useTheme } from "@mui/material/styles";
import CloseRoundedIcon from "@mui/icons-material/CloseRounded";
import FavoriteRoundedIcon from "@mui/icons-material/FavoriteRounded";
import FavoriteBorderRoundedIcon from "@mui/icons-material/FavoriteBorderRounded";
import NavigationRoundedIcon from "@mui/icons-material/NavigationRounded";
import MapRoundedIcon from "@mui/icons-material/MapRounded";
import FlagRoundedIcon from "@mui/icons-material/FlagRounded";
import EditRoundedIcon from "@mui/icons-material/EditRounded";
import ThumbUpRoundedIcon from "@mui/icons-material/ThumbUpRounded";
import ThumbUpOffAltRoundedIcon from "@mui/icons-material/ThumbUpOffAltRounded";
import PlaceRoundedIcon from "@mui/icons-material/PlaceRounded";
import { api } from "../api/client";
import type { ChargerStatus, StationDetail } from "../api/types";
import StationReviews from "./StationReviews";
import { statusDot, statusLabel } from "../theme";
import { useAuth } from "../auth/AuthContext";

interface Props {
  stationId: string | null;
  onClose: () => void;
  isFavorite: boolean;
  onToggleFavorite: (id: string, next: boolean) => void;
  onRequireLogin: () => void;
  /**
   * Compute + draw an in-app driving route to the given destination and return a
   * distance/duration summary (or null if it couldn't be computed).
   */
  onNavigate: (
    destination: { lat: number; lng: number; name: string; id: string },
  ) => Promise<{ distanceText: string; durationText: string; trafficAware: boolean } | null>;
}

const REPORT_STATUSES: ChargerStatus[] = ["AVAILABLE", "OCCUPIED", "BROKEN", "CLOSED"];

function timeAgo(iso: string | null): string {
  if (!iso) return "unknown";
  const mins = Math.round((Date.now() - new Date(iso).getTime()) / 60000);
  if (mins < 1) return "just now";
  if (mins < 60) return `${mins} min ago`;
  const h = Math.round(mins / 60);
  if (h < 24) return `${h} h ago`;
  return `${Math.round(h / 24)} d ago`;
}

/** Small overline label used above stat values. */
function Overline({ children }: { children: React.ReactNode }) {
  return (
    <Typography
      sx={{
        fontSize: 11,
        fontWeight: 600,
        letterSpacing: 0.4,
        textTransform: "uppercase",
        color: "text.secondary",
      }}
    >
      {children}
    </Typography>
  );
}

export default function StationDetailSheet({
  stationId,
  onClose,
  isFavorite,
  onToggleFavorite,
  onRequireLogin,
  onNavigate,
}: Props) {
  const { user } = useAuth();
  const theme = useTheme();
  const isDesktop = useMediaQuery(theme.breakpoints.up("md"));
  const [station, setStation] = useState<StationDetail | null>(null);
  const [reportOpen, setReportOpen] = useState(false);
  const [reportStatus, setReportStatus] = useState<ChargerStatus>("OCCUPIED");
  const [reportCharger, setReportCharger] = useState<string>("");
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [routing, setRouting] = useState(false);
  const [routeError, setRouteError] = useState<string | null>(null);
  // Suggest-an-edit form (user-proposed changes queued for admin approval).
  const [editOpen, setEditOpen] = useState(false);
  const [editName, setEditName] = useState("");
  const [editOperator, setEditOperator] = useState("");
  const [editAddress, setEditAddress] = useState("");
  const [editPrice, setEditPrice] = useState("");
  const [editSaving, setEditSaving] = useState(false);
  const [likeCount, setLikeCount] = useState(0);
  const [likedByMe, setLikedByMe] = useState(false);
  const [likeBusy, setLikeBusy] = useState(false);

  useEffect(() => {
    setMessage(null);
    setError(null);
    setStation(null);
    setReportOpen(false);
    setEditOpen(false);
    setRouteError(null);
    setRouting(false);
    setLikeCount(0);
    setLikedByMe(false);
    if (stationId) {
      api.station(stationId).then(setStation).catch(() => setError("Failed to load station"));
      api
        .likeStatus(stationId)
        .then((s) => {
          setLikeCount(s.likeCount);
          setLikedByMe(s.likedByMe);
        })
        .catch(() => {});
    }
  }, [stationId]);

  const toggleLike = async () => {
    if (!stationId) return;
    if (!user) return onRequireLogin();
    setLikeBusy(true);
    // optimistic
    setLikedByMe((v) => !v);
    setLikeCount((c) => c + (likedByMe ? -1 : 1));
    try {
      const s = await api.toggleLike(stationId);
      setLikeCount(s.likeCount);
      setLikedByMe(s.likedByMe);
    } catch {
      // revert on failure
      setLikedByMe((v) => !v);
      setLikeCount((c) => c + (likedByMe ? 1 : -1));
    } finally {
      setLikeBusy(false);
    }
  };

  // Prefill the edit form with the station's current values when it opens.
  useEffect(() => {
    if (editOpen && station) {
      setEditName(station.name ?? "");
      setEditOperator(station.operator ?? "");
      setEditAddress(station.address ?? "");
      setEditPrice(
        station.currentPricing?.pricePerKwh != null
          ? String(station.currentPricing.pricePerKwh)
          : "",
      );
    }
  }, [editOpen, station]);

  const submitEdit = async () => {
    if (!stationId || !station) return;
    if (!user) return onRequireLogin();
    // Only send fields that actually changed from the station's current values.
    const changes: {
      name?: string;
      operator?: string;
      address?: string;
      pricePerKwh?: number;
    } = {};
    if (editName.trim() && editName.trim() !== (station.name ?? "")) changes.name = editName.trim();
    if (editOperator.trim() !== (station.operator ?? "")) changes.operator = editOperator.trim();
    if (editAddress.trim() !== (station.address ?? "")) changes.address = editAddress.trim();
    const priceNum = editPrice === "" ? null : Number(editPrice);
    const currentPrice = station.currentPricing?.pricePerKwh ?? null;
    if (priceNum != null && priceNum !== currentPrice) changes.pricePerKwh = priceNum;

    if (Object.keys(changes).length === 0) {
      setError("No changes to submit.");
      return;
    }
    setEditSaving(true);
    try {
      await api.proposeStationEdit(stationId, changes);
      setMessage("Thanks! Your changes were submitted for admin approval.");
      setError(null);
      setEditOpen(false);
    } catch {
      setError("Could not submit your changes.");
    } finally {
      setEditSaving(false);
    }
  };

  const submitReport = async () => {
    if (!stationId) return;
    if (!user) return onRequireLogin();
    try {
      const res = await api.report(stationId, reportStatus, reportCharger || undefined);
      setMessage(res.message);
      setError(null);
      setStation(await api.station(stationId));
      setReportOpen(false);
    } catch {
      setError("Could not submit report");
    }
  };

  const openGoogleMaps = () => {
    if (!station?.location) return;
    // External directions are also a navigate action — signed-in only.
    if (!user) return onRequireLogin();
    const { lat, lng } = station.location;
    // Exact coordinates as destination (encoded comma) so Google doesn't snap to a
    // nearby named place. Google provides real live-traffic, least-traffic routing.
    const dest = encodeURIComponent(`${lat},${lng}`);
    window.open(
      `https://www.google.com/maps/dir/?api=1&destination=${dest}&travelmode=driving`,
      "_blank",
    );
  };

  const startNavigate = async () => {
    if (!station?.location) return;
    // Navigation is a signed-in-only action; viewing the station is open to everyone.
    if (!user) return onRequireLogin();
    setRouting(true);
    setRouteError(null);
    try {
      const info = await onNavigate({
        lat: station.location.lat,
        lng: station.location.lng,
        name: station.name,
        id: station.id,
      });
      if (info) {
        // Route is drawn + the persistent ETA panel shows the details, so close the
        // sheet to reveal the map and the route.
        onClose();
      } else {
        setRouteError("Couldn't get a route. Check location permission and try again.");
      }
    } finally {
      setRouting(false);
    }
  };

  const dot = station ? statusDot(station.availabilitySummary) : "#9ca3af";

  return (
    <Drawer
      variant={isDesktop ? "persistent" : "temporary"}
      anchor={isDesktop ? "right" : "bottom"}
      open={!!stationId}
      onClose={onClose}
      sx={{ zIndex: 1360 }}
      PaperProps={{
        sx: isDesktop
          ? {
              // Desktop: right-side panel. Its bottom stops above the nearby-charger sheet
              // (~20% peek) + bottom nav (64px) so it never overlaps them. `persistent`
              // means no modal backdrop, so the map + Add-station button stay usable.
              width: 400,
              maxWidth: "calc(100vw - 24px)",
              top: "calc(var(--safe-top) + 58px)",
              height: "auto",
              // Sit just above the nearby-charger sheet (its ~20% peek), almost touching.
              bottom: "calc(20% + 4px)",
              right: 12,
              borderTopLeftRadius: 16,
              borderTopRightRadius: 16,
              borderBottomLeftRadius: 16,
              borderBottomRightRadius: 16,
              border: "1px solid rgba(30,60,40,0.10)",
              bgcolor: "#ffffff",
              boxShadow: "0 12px 40px rgba(0,0,0,0.16)",
            }
          : {
              // Mobile: bottom sheet, full width (no clipping).
              borderTopLeftRadius: 20,
              borderTopRightRadius: 20,
              maxHeight: "90%",
              width: "100%",
              bgcolor: "#ffffff",
              boxShadow: "0 -8px 32px rgba(0,0,0,0.12)",
            },
      }}
    >
      {/* Grabber */}
      {!isDesktop && (
        <Box sx={{ display: "flex", justifyContent: "center", pt: 1.25, pb: 0.5 }}>
          <Box sx={{ width: 36, height: 4, borderRadius: 2, bgcolor: "#e2e5e9" }} />
        </Box>
      )}

      {error && <Alert severity="error" sx={{ mx: 2.5, mt: 1 }}>{error}</Alert>}

      {station && (
        <Box
          sx={{
            display: "flex",
            flexDirection: "column",
            overflow: "hidden",
            height: isDesktop ? "100%" : "auto",
            maxHeight: isDesktop ? "100%" : "calc(90vh - 20px)",
            pt: isDesktop ? 2 : 0,
          }}
        >
          <Box sx={{ overflowY: "auto", px: 2.5, pt: 1 }}>
            {/* Header: name + address left, actions right */}
            <Stack direction="row" alignItems="flex-start" justifyContent="space-between" spacing={1}>
              <Box sx={{ minWidth: 0 }}>
                <Typography sx={{ fontSize: 20, fontWeight: 700, lineHeight: 1.2 }} noWrap>
                  {station.name}
                </Typography>
                {station.address && (
                  <Stack direction="row" spacing={0.5} alignItems="center" mt={0.5}>
                    <PlaceRoundedIcon sx={{ fontSize: 15, color: "text.secondary" }} />
                    <Typography variant="body2" color="text.secondary" noWrap>
                      {station.address}
                    </Typography>
                  </Stack>
                )}
              </Box>
              <Stack direction="row" spacing={0.5} sx={{ flexShrink: 0 }}>
                <Button
                  size="small"
                  onClick={toggleLike}
                  disabled={likeBusy}
                  startIcon={
                    likedByMe ? (
                      <ThumbUpRoundedIcon sx={{ fontSize: 16 }} />
                    ) : (
                      <ThumbUpOffAltRoundedIcon sx={{ fontSize: 16 }} />
                    )
                  }
                  sx={{
                    minWidth: 0,
                    px: 1.25,
                    height: 40,
                    borderRadius: 999,
                    bgcolor: likedByMe ? "rgba(31,157,87,.12)" : "#f3f4f6",
                    color: likedByMe ? "primary.main" : "text.secondary",
                  }}
                >
                  {likeCount}
                </Button>
                <IconButton
                  size="small"
                  onClick={() =>
                    user ? onToggleFavorite(station.id, !isFavorite) : onRequireLogin()
                  }
                  sx={{ width: 40, height: 40, bgcolor: "#f3f4f6" }}
                >
                  {isFavorite ? (
                    <FavoriteRoundedIcon sx={{ color: "#dc2626" }} fontSize="small" />
                  ) : (
                    <FavoriteBorderRoundedIcon fontSize="small" />
                  )}
                </IconButton>
                <IconButton
                  size="small"
                  onClick={onClose}
                  sx={{ width: 40, height: 40, bgcolor: "#f3f4f6" }}
                >
                  <CloseRoundedIcon fontSize="small" />
                </IconButton>
              </Stack>
            </Stack>

            {/* Status line: dot + text */}
            <Stack direction="row" spacing={1} alignItems="center" mt={1.5}>
              <Box sx={{ width: 8, height: 8, borderRadius: "50%", bgcolor: dot }} />
              <Typography sx={{ fontSize: 14, fontWeight: 600 }}>
                {station.availableCount} of {station.totalChargers} available
              </Typography>
              <Typography variant="body2" color="text.secondary">
                · updated {timeAgo(station.availabilityUpdatedAt)}
              </Typography>
            </Stack>

            {/* Stat strip: values split by hairline dividers, no boxes */}
            <Stack
              direction="row"
              divider={<Divider orientation="vertical" flexItem />}
              spacing={0}
              sx={{ mt: 2, mb: 2 }}
            >
              <Box sx={{ flex: 1, pr: 2 }}>
                <Overline>Price</Overline>
                <Typography sx={{ fontSize: 18, fontWeight: 700 }}>
                  {station.currentPricing ? `₱${station.currentPricing.pricePerKwh}` : "—"}
                  <Typography component="span" variant="caption" color="text.secondary">
                    {" "}/kWh
                  </Typography>
                </Typography>
              </Box>
              <Box sx={{ flex: 1, px: 2 }}>
                <Overline>Distance</Overline>
                <Typography sx={{ fontSize: 18, fontWeight: 700 }}>
                  {station.distanceMeters != null
                    ? `${(station.distanceMeters / 1000).toFixed(1)} km`
                    : "—"}
                </Typography>
              </Box>
              <Box sx={{ flex: 1, pl: 2 }}>
                <Overline>Hours</Overline>
                <Typography sx={{ fontSize: 15, fontWeight: 600 }} noWrap>
                  {station.openingHours ?? "—"}
                </Typography>
              </Box>
            </Stack>

            <Divider />

            {/* Chargers: divider-separated rows, no boxes */}
            <Typography sx={{ fontSize: 13, fontWeight: 700, mt: 2, mb: 0.5 }}>
              CHARGERS
            </Typography>
            <Stack divider={<Divider />}>
              {station.chargers.map((c) => (
                <Stack
                  key={c.chargerId}
                  direction="row"
                  alignItems="center"
                  justifyContent="space-between"
                  sx={{ py: 1.5 }}
                >
                  <Box>
                    <Typography sx={{ fontSize: 15, fontWeight: 600 }}>
                      {c.connectorType}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {c.chargerType.replace("_", " ")} · {c.powerKw} kW
                    </Typography>
                  </Box>
                  <Stack direction="row" spacing={0.75} alignItems="center">
                    <Box
                      sx={{ width: 8, height: 8, borderRadius: "50%", bgcolor: statusDot(c.status) }}
                    />
                    <Typography
                      sx={{ fontSize: 13, fontWeight: 600, color: statusDot(c.status) }}
                    >
                      {statusLabel(c.status)}
                    </Typography>
                  </Stack>
                </Stack>
              ))}
            </Stack>

            {/* Report (collapsible, secondary) */}
            {message && <Alert severity="success" sx={{ my: 1.5 }}>{message}</Alert>}
            <Collapse in={reportOpen}>
              <Box sx={{ pt: 1, pb: 0.5 }}>
                <Stack direction="row" spacing={1}>
                  <TextField
                    select
                    id="report-status"
                    size="small"
                    label="Status"
                    value={reportStatus}
                    onChange={(e) => setReportStatus(e.target.value as ChargerStatus)}
                    sx={{ flex: 1 }}
                  >
                    {REPORT_STATUSES.map((s) => (
                      <MenuItem key={s} value={s}>
                        {statusLabel(s)}
                      </MenuItem>
                    ))}
                  </TextField>
                  <TextField
                    select
                    id="report-charger"
                    size="small"
                    label="Charger"
                    value={reportCharger}
                    onChange={(e) => setReportCharger(e.target.value)}
                    sx={{ flex: 1 }}
                  >
                    <MenuItem value="">Whole station</MenuItem>
                    {station.chargers.map((c, i) => (
                      <MenuItem key={c.chargerId} value={c.chargerId}>
                        #{i + 1} {c.connectorType}
                      </MenuItem>
                    ))}
                  </TextField>
                </Stack>
                <Button fullWidth variant="contained" onClick={submitReport} sx={{ mt: 1.5 }}>
                  Submit report
                </Button>
              </Box>
            </Collapse>

            {/* Suggest an edit (collapsible). Changes are queued for admin approval. */}
            <Collapse in={editOpen}>
              <Box sx={{ pt: 1, pb: 0.5 }}>
                <Alert severity="info" sx={{ mb: 1.5 }}>
                  Your changes go to admins for approval before they show on the map.
                </Alert>
                <Stack spacing={1.25}>
                  <TextField
                    size="small"
                    label="Station name"
                    value={editName}
                    onChange={(e) => setEditName(e.target.value)}
                    fullWidth
                  />
                  <TextField
                    size="small"
                    label="Operator"
                    value={editOperator}
                    onChange={(e) => setEditOperator(e.target.value)}
                    fullWidth
                  />
                  <TextField
                    size="small"
                    label="Address"
                    value={editAddress}
                    onChange={(e) => setEditAddress(e.target.value)}
                    fullWidth
                  />
                  <TextField
                    size="small"
                    type="number"
                    label="Price (₱/kWh)"
                    value={editPrice}
                    onChange={(e) => setEditPrice(e.target.value)}
                    fullWidth
                  />
                  <Button
                    fullWidth
                    variant="contained"
                    onClick={submitEdit}
                    disabled={editSaving}
                  >
                    {editSaving ? "Submitting…" : "Submit for approval"}
                  </Button>
                </Stack>
              </Box>
            </Collapse>

            <Divider sx={{ my: 2 }} />

            {/* Reviews / comments */}
            <StationReviews stationId={station.id} onRequireLogin={onRequireLogin} />
          </Box>

          {/* Bottom-pinned actions */}
          <Box
            sx={{
              px: 2.5,
              pt: 1.5,
              pb: "calc(var(--safe-bottom) + 16px)",
              borderTop: "1px solid",
              borderColor: "divider",
              bgcolor: "#fff",
            }}
          >
            {routeError && (
              <Alert severity="warning" sx={{ mb: 1.5 }}>
                {routeError}
              </Alert>
            )}

            <Stack direction="row" spacing={1} alignItems="center">
              <Button
                variant="text"
                onClick={() => {
                  if (!user) return onRequireLogin();
                  setReportOpen((o) => !o);
                  setEditOpen(false);
                }}
                startIcon={<FlagRoundedIcon />}
                sx={{ flexShrink: 0, minWidth: 0, px: 1 }}
              >
                Report
              </Button>
              <Button
                variant="text"
                onClick={() => {
                  if (!user) return onRequireLogin();
                  setEditOpen((o) => !o);
                  setReportOpen(false);
                }}
                startIcon={<EditRoundedIcon />}
                sx={{ flexShrink: 0, minWidth: 0, px: 1 }}
              >
                Edit
              </Button>
              <Button
                fullWidth
                variant="contained"
                size="large"
                startIcon={<NavigationRoundedIcon />}
                onClick={startNavigate}
                disabled={!station.location || routing}
              >
                {routing ? "Routing…" : "Navigate"}
              </Button>
              <Tooltip title="Open in Google Maps">
                <span>
                  <IconButton
                    onClick={openGoogleMaps}
                    disabled={!station.location}
                    sx={{
                      flexShrink: 0,
                      border: "1px solid",
                      borderColor: "divider",
                      borderRadius: 2,
                      width: 44,
                      height: 44,
                    }}
                  >
                    <MapRoundedIcon />
                  </IconButton>
                </span>
              </Tooltip>
            </Stack>
          </Box>
        </Box>
      )}
    </Drawer>
  );
}
