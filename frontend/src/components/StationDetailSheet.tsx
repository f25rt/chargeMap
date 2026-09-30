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
import { hud, statusDot, statusLabel } from "../theme";
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

/** A boxed HUD telemetry readout (label + big value + optional unit/sub). */
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
        bgcolor: hud.surface2,
        border: `1px solid ${hud.border}`,
        borderRadius: 1,
        p: 1.25,
      }}
    >
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
        {label}
      </Typography>
      <Typography
        sx={{
          fontFamily: "'JetBrains Mono', monospace",
          fontWeight: 700,
          fontSize: 17,
          lineHeight: 1.1,
          color: accent,
          mt: 0.4,
        }}
        noWrap
      >
        {value}
        {unit && (
          <Typography component="span" sx={{ fontSize: 10, color: hud.textMuted, ml: 0.4 }}>
            {unit}
          </Typography>
        )}
      </Typography>
      {sub && (
        <Typography
          sx={{
            fontFamily: "'JetBrains Mono', monospace",
            fontSize: 9,
            color: hud.textMuted,
            textTransform: "uppercase",
            mt: 0.25,
          }}
          noWrap
        >
          {sub}
        </Typography>
      )}
    </Box>
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

  const dot = station ? statusDot(station.availabilitySummary) : hud.textMuted;

  // Peak power across the station's chargers, and the type that delivers it.
  const topCharger =
    station && station.chargers.length > 0
      ? station.chargers.reduce((a, b) => (b.powerKw > a.powerKw ? b : a))
      : null;
  const maxPowerKw = topCharger ? topCharger.powerKw : 0;
  const maxChargerType = topCharger ? topCharger.chargerType : null;

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
              borderTopLeftRadius: 8,
              borderTopRightRadius: 8,
              borderBottomLeftRadius: 8,
              borderBottomRightRadius: 8,
              border: `1px solid ${hud.borderMint}`,
              bgcolor: hud.surface1,
              boxShadow: `0 0 32px rgba(0,255,157,0.10)`,
            }
          : {
              // Mobile: bottom sheet, full width (no clipping).
              borderTopLeftRadius: 12,
              borderTopRightRadius: 12,
              maxHeight: "90%",
              width: "100%",
              bgcolor: hud.surface1,
              borderTop: `1px solid ${hud.borderMint}`,
              boxShadow: `0 0 32px rgba(0,255,157,0.12)`,
            },
      }}
    >
      {/* Grabber */}
      {!isDesktop && (
        <Box sx={{ display: "flex", justifyContent: "center", pt: 1.25, pb: 0.5 }}>
          <Box sx={{ width: 36, height: 4, borderRadius: 2, bgcolor: hud.border }} />
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
            {/* Hero header: operator/id/status meta chips */}
            <Stack
              direction="row"
              alignItems="center"
              spacing={1}
              flexWrap="wrap"
              useFlexGap
              sx={{ mb: 1 }}
            >
              {station.operator && (
                <Typography
                  sx={{
                    fontFamily: "'JetBrains Mono', monospace",
                    fontSize: 10,
                    letterSpacing: "0.08em",
                    textTransform: "uppercase",
                    color: hud.textMuted,
                    bgcolor: hud.surface2,
                    px: 0.75,
                    py: 0.25,
                    borderRadius: 0.5,
                    border: `1px solid ${hud.border}`,
                  }}
                >
                  {station.operator}
                </Typography>
              )}
              <Typography
                sx={{
                  fontFamily: "'JetBrains Mono', monospace",
                  fontSize: 10,
                  letterSpacing: "0.08em",
                  color: hud.textMuted,
                }}
              >
                ID: {station.id.slice(-6).toUpperCase()}
              </Typography>
              <Box sx={{ flex: 1 }} />
              <Stack direction="row" spacing={0.5} alignItems="center">
                <Box sx={{ width: 7, height: 7, borderRadius: "50%", bgcolor: dot }} />
                <Typography
                  sx={{
                    fontFamily: "'JetBrains Mono', monospace",
                    fontSize: 11,
                    fontWeight: 700,
                    color: dot,
                  }}
                >
                  {station.availableCount}/{station.totalChargers} FREE
                </Typography>
              </Stack>
              <IconButton
                size="small"
                onClick={onClose}
                sx={{ width: 30, height: 30, bgcolor: hud.surface2, ml: 0.5 }}
              >
                <CloseRoundedIcon sx={{ fontSize: 16 }} />
              </IconButton>
            </Stack>

            {/* Title */}
            <Typography sx={{ fontSize: 22, fontWeight: 700, lineHeight: 1.15 }}>
              {station.name}
            </Typography>

            {/* Distance / hours / updated meta line */}
            <Stack
              direction="row"
              spacing={1.5}
              alignItems="center"
              flexWrap="wrap"
              useFlexGap
              sx={{ mt: 0.75, color: hud.textMuted, fontFamily: "'JetBrains Mono', monospace" }}
            >
              {station.distanceMeters != null && (
                <Stack direction="row" spacing={0.5} alignItems="center">
                  <PlaceRoundedIcon sx={{ fontSize: 14, color: hud.mint }} />
                  <Typography sx={{ fontSize: 12 }}>
                    {(station.distanceMeters / 1000).toFixed(1)} km away
                  </Typography>
                </Stack>
              )}
              {station.openingHours && (
                <Typography sx={{ fontSize: 12 }}>• {station.openingHours}</Typography>
              )}
              <Typography sx={{ fontSize: 12 }}>
                • updated {timeAgo(station.availabilityUpdatedAt)}
              </Typography>
            </Stack>

            {/* Telemetry stat strip: MAX OUTPUT / UNIT RATE / AVAILABILITY */}
            <Box
              sx={{
                mt: 2,
                display: "grid",
                gridTemplateColumns: "1fr 1fr 1fr",
                gap: 1,
              }}
            >
              <TelemetryStat
                label="Max Output"
                value={String(maxPowerKw)}
                unit="kW"
                accent={hud.mint}
                sub={maxChargerType ? maxChargerType.replace("_", " ") : undefined}
              />
              <TelemetryStat
                label="Unit Rate"
                value={station.currentPricing ? `₱${station.currentPricing.pricePerKwh}` : "—"}
                unit={station.currentPricing ? "per kWh" : undefined}
                accent={hud.textHigh}
              />
              <TelemetryStat
                label="Availability"
                value={statusLabel(station.availabilitySummary)}
                accent={dot}
              />
            </Box>

            {/* Connector stalls */}
            <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mt: 2.5 }}>
              <Typography
                sx={{
                  fontFamily: "'JetBrains Mono', monospace",
                  fontSize: 11,
                  fontWeight: 700,
                  letterSpacing: "0.1em",
                  color: hud.textMuted,
                }}
              >
                CONNECTOR STALLS ({station.chargers.length} BAYS)
              </Typography>
            </Stack>
            <Stack spacing={1} sx={{ mt: 1 }}>
              {station.chargers.map((c, i) => {
                const cDot = statusDot(c.status);
                return (
                  <Box
                    key={c.chargerId}
                    sx={{
                      p: 1.25,
                      borderRadius: 1,
                      bgcolor: hud.surface2,
                      border: `1px solid ${hud.border}`,
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "space-between",
                      gap: 1,
                    }}
                  >
                    <Box sx={{ minWidth: 0 }}>
                      <Stack direction="row" spacing={1} alignItems="center">
                        <Typography sx={{ fontSize: 14, fontWeight: 700 }}>
                          {c.connectorType}
                        </Typography>
                        <Typography
                          sx={{
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: 10,
                            fontWeight: 700,
                            color: hud.void,
                            bgcolor: hud.mint,
                            px: 0.6,
                            py: 0.1,
                            borderRadius: 0.5,
                          }}
                        >
                          {c.powerKw} kW
                        </Typography>
                      </Stack>
                      <Typography
                        sx={{
                          fontFamily: "'JetBrains Mono', monospace",
                          fontSize: 11,
                          color: hud.textMuted,
                          mt: 0.25,
                        }}
                      >
                        {c.chargerType.replace("_", " ")} • Bay {String(i + 1).padStart(2, "0")}
                      </Typography>
                    </Box>
                    <Stack direction="row" spacing={0.6} alignItems="center" sx={{ flexShrink: 0 }}>
                      <Box sx={{ width: 7, height: 7, borderRadius: "50%", bgcolor: cDot }} />
                      <Typography
                        sx={{
                          fontFamily: "'JetBrains Mono', monospace",
                          fontSize: 11,
                          fontWeight: 700,
                          color: cDot,
                          textTransform: "uppercase",
                        }}
                      >
                        {statusLabel(c.status)}
                      </Typography>
                    </Stack>
                  </Box>
                );
              })}
            </Stack>

            {/* Station amenities */}
            {station.amenities.length > 0 && (
              <>
                <Typography
                  sx={{
                    fontFamily: "'JetBrains Mono', monospace",
                    fontSize: 11,
                    fontWeight: 700,
                    letterSpacing: "0.1em",
                    color: hud.textMuted,
                    mt: 2.5,
                    mb: 1,
                  }}
                >
                  STATION AMENITIES
                </Typography>
                <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap>
                  {station.amenities.map((a) => (
                    <Typography
                      key={a}
                      sx={{
                        fontFamily: "'JetBrains Mono', monospace",
                        fontSize: 11,
                        color: hud.textHigh,
                        bgcolor: hud.surface2,
                        border: `1px solid ${hud.border}`,
                        px: 1,
                        py: 0.4,
                        borderRadius: 0.5,
                        textTransform: "capitalize",
                      }}
                    >
                      {a}
                    </Typography>
                  ))}
                </Stack>
              </>
            )}

            {/* Site visual verification (station photo) */}
            {station.imageId && (
              <>
                <Typography
                  sx={{
                    fontFamily: "'JetBrains Mono', monospace",
                    fontSize: 11,
                    fontWeight: 700,
                    letterSpacing: "0.1em",
                    color: hud.textMuted,
                    mt: 2.5,
                    mb: 1,
                  }}
                >
                  SITE VISUAL VERIFICATION
                </Typography>
                <Box
                  component="img"
                  src={api.imageUrl(station.imageId)}
                  alt={station.name}
                  sx={{
                    width: "100%",
                    borderRadius: 1,
                    border: `1px solid ${hud.border}`,
                    objectFit: "cover",
                    maxHeight: 180,
                  }}
                />
              </>
            )}

            <Divider sx={{ mt: 2 }} />

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
              bgcolor: hud.surface1,
            }}
          >
            {routeError && (
              <Alert severity="warning" sx={{ mb: 1.5 }}>
                {routeError}
              </Alert>
            )}

            {/* Secondary actions: report / edit (text), like count */}
            <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
              <Button
                variant="text"
                size="small"
                onClick={() => {
                  if (!user) return onRequireLogin();
                  setReportOpen((o) => !o);
                  setEditOpen(false);
                }}
                startIcon={<FlagRoundedIcon />}
                sx={{ flexShrink: 0, minWidth: 0, px: 1, color: hud.textMuted }}
              >
                Report
              </Button>
              <Button
                variant="text"
                size="small"
                onClick={() => {
                  if (!user) return onRequireLogin();
                  setEditOpen((o) => !o);
                  setReportOpen(false);
                }}
                startIcon={<EditRoundedIcon />}
                sx={{ flexShrink: 0, minWidth: 0, px: 1, color: hud.textMuted }}
              >
                Edit
              </Button>
              <Box sx={{ flex: 1 }} />
              <Button
                size="small"
                onClick={toggleLike}
                disabled={likeBusy}
                startIcon={
                  likedByMe ? (
                    <ThumbUpRoundedIcon sx={{ fontSize: 15 }} />
                  ) : (
                    <ThumbUpOffAltRoundedIcon sx={{ fontSize: 15 }} />
                  )
                }
                sx={{
                  minWidth: 0,
                  px: 1,
                  color: likedByMe ? "primary.main" : hud.textMuted,
                }}
              >
                {likeCount}
              </Button>
            </Stack>

            {/* Primary action row: save / share + big START NAVIGATION */}
            <Stack direction="row" spacing={1} alignItems="center">
              <IconButton
                onClick={() =>
                  user ? onToggleFavorite(station.id, !isFavorite) : onRequireLogin()
                }
                sx={{
                  flexShrink: 0,
                  border: `1px solid ${hud.border}`,
                  borderRadius: 1,
                  width: 48,
                  height: 48,
                  bgcolor: hud.surface2,
                }}
              >
                {isFavorite ? (
                  <FavoriteRoundedIcon sx={{ color: hud.neon }} fontSize="small" />
                ) : (
                  <FavoriteBorderRoundedIcon fontSize="small" />
                )}
              </IconButton>
              <Tooltip title="Open in Google Maps">
                <span>
                  <IconButton
                    onClick={openGoogleMaps}
                    disabled={!station.location}
                    sx={{
                      flexShrink: 0,
                      border: `1px solid ${hud.border}`,
                      borderRadius: 1,
                      width: 48,
                      height: 48,
                      bgcolor: hud.surface2,
                    }}
                  >
                    <MapRoundedIcon fontSize="small" />
                  </IconButton>
                </span>
              </Tooltip>
              <Button
                fullWidth
                variant="contained"
                size="large"
                startIcon={<NavigationRoundedIcon />}
                onClick={startNavigate}
                disabled={!station.location || routing}
                sx={{ height: 48, boxShadow: "0 0 20px rgba(0,255,157,.35)" }}
              >
                {routing
                  ? "Routing…"
                  : station.distanceMeters != null
                    ? `Start Navigation • ${(station.distanceMeters / 1000).toFixed(1)} km`
                    : "Start Navigation"}
              </Button>
            </Stack>
          </Box>
        </Box>
      )}
    </Drawer>
  );
}
