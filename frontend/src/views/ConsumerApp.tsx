import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  Avatar,
  Badge,
  BottomNavigation,
  BottomNavigationAction,
  Box,
  Button,
  Chip,
  CircularProgress,
  Collapse,
  Divider,
  TextField,
  IconButton,
  InputBase,
  Paper,
  Stack,
  Typography,
} from "@mui/material";
import ExploreIcon from "@mui/icons-material/Explore";
import SavingsIcon from "@mui/icons-material/Savings";
import BoltIcon from "@mui/icons-material/Bolt";
import FavoriteIcon from "@mui/icons-material/Favorite";
import SearchIcon from "@mui/icons-material/Search";
import TuneIcon from "@mui/icons-material/Tune";
import PersonIcon from "@mui/icons-material/Person";
import ClearIcon from "@mui/icons-material/Clear";
import PlaceRoundedIcon from "@mui/icons-material/PlaceRounded";
import NavigationRoundedIcon from "@mui/icons-material/NavigationRounded";
import {
  fetchRoute,
  formatDistance,
  formatDuration,
  estimateMinutes,
  RANGE_SAFETY_BUFFER,
  AVG_SPEED_KMH,
} from "../routing/osrm";
import { api } from "../api/client";
import type { StationFilters, StationSummary } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import StationMap from "../components/StationMap";
import StationCard from "../components/StationCard";
import FilterBar from "../components/FilterBar";
import StationDetailSheet from "../components/StationDetailSheet";
import AuthDialog from "../components/AuthDialog";
import BottomSheet from "../components/BottomSheet";
import AddStationDialog from "../components/AddStationDialog";
import ProfileSheet from "../components/ProfileSheet";
import { hud } from "../theme";
import { Fab } from "@mui/material";
import AddRoundedIcon from "@mui/icons-material/AddRounded";

// Cebu Business Park — default map center for the MVP launch area.
const CEBU: [number, number] = [10.3181, 123.9068];
const RADIUS_KM = 10;
const BOTTOM_NAV_HEIGHT = 64;

type View = "nearby" | "cheapest" | "available" | "favorites";

const VIEW_LABEL: Record<View, string> = {
  nearby: "Nearby chargers",
  cheapest: "Cheapest nearby",
  available: "Available now",
  favorites: "Your favorites",
};

export default function ConsumerApp() {
  const { user } = useAuth();
  const [view, setView] = useState<View>("nearby");
  const [stations, setStations] = useState<StationSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState<StationFilters>({});
  const [showFilters, setShowFilters] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [searching, setSearching] = useState(false);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [authOpen, setAuthOpen] = useState(false);
  const [addOpen, setAddOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const [pinMode, setPinMode] = useState(false);
  const [pinnedPoint, setPinnedPoint] = useState<[number, number] | null>(null);
  const [route, setRoute] = useState<[number, number][] | null>(null);
  const [origin, setOrigin] = useState<[number, number] | null>(null);
  const [routeSummary, setRouteSummary] = useState<{
    stationName: string;
    stationId: string;
    distanceText: string;
    durationText: string;
    durationSeconds: number;
    trafficAware: boolean;
  } | null>(null);
  // Battery driving range the user has left, in minutes (drives reachability + suggestions).
  const [batteryMinutes, setBatteryMinutes] = useState<number | null>(null);
  const [suggestion, setSuggestion] = useState<{
    station: StationSummary;
    etaMinutes: number;
  } | null>(null);
  // Live ETA (seconds) that counts down and recomputes as the device moves.
  const [liveEtaSeconds, setLiveEtaSeconds] = useState<number | null>(null);
  // User's current location, used to estimate drive time to nearby chargers.
  const [userLoc, setUserLoc] = useState<[number, number] | null>(null);
  // Live height fraction of the nearby-charger bottom sheet (drives the FAB position).
  const [sheetFraction, setSheetFraction] = useState(0.2);
  const [favoriteIds, setFavoriteIds] = useState<Set<string>>(new Set());

  const [lat, lng] = CEBU;

  const load = useCallback(
    async (silent = false) => {
      if (!silent) {
        setLoading(true);
        setSearching(false);
      }
      try {
        let data: StationSummary[];
        switch (view) {
          case "nearby":
            data = await api.nearby(lat, lng, RADIUS_KM, filters);
            break;
          case "cheapest":
            data = await api.cheapest(lat, lng, RADIUS_KM, filters);
            break;
          case "available":
            data = await api.available(lat, lng, RADIUS_KM, filters);
            break;
          case "favorites":
            data = user ? await api.favorites() : [];
            break;
          default:
            data = await api.nearby(lat, lng, RADIUS_KM, filters);
        }
        setStations(data);
      } finally {
        if (!silent) setLoading(false);
      }
    },
    [view, filters, lat, lng, user],
  );

  useEffect(() => {
    if (!searching) load();
  }, [load, searching]);

  // Poll for station changes so admin-approved / updated stations propagate to every
  // viewer's map in near-real-time without a manual reload. Silent (no spinner).
  useEffect(() => {
    if (searching) return; // don't clobber a user's active search results
    const id = setInterval(() => {
      void load(true);
    }, 15000);
    return () => clearInterval(id);
  }, [load, searching]);

  const loadFavorites = useCallback(async () => {
    if (!user) {
      setFavoriteIds(new Set());
      return;
    }
    const favs = await api.favorites();
    setFavoriteIds(new Set(favs.map((f) => f.id)));
  }, [user]);

  useEffect(() => {
    loadFavorites();
  }, [loadFavorites]);

  const runSearch = async () => {
    if (searchTerm.trim().length < 2) return;
    setLoading(true);
    setSearching(true);
    try {
      setStations(await api.search(searchTerm.trim()));
    } finally {
      setLoading(false);
    }
  };

  const clearSearch = () => {
    setSearchTerm("");
    setSearching(false);
    load();
  };

  const toggleFavorite = async (id: string, next: boolean) => {
    if (next) await api.addFavorite(id);
    else await api.removeFavorite(id);
    await loadFavorites();
    if (view === "favorites") await load();
  };

  const center = useMemo<[number, number]>(() => {
    const withLoc = stations.find((s) => s.location);
    return withLoc?.location ? [withLoc.location.lat, withLoc.location.lng] : CEBU;
  }, [stations]);

  const activeFilterCount = useMemo(
    () =>
      Object.entries(filters).filter(([, v]) => v !== "" && v != null && v !== false).length,
    [filters],
  );

  const startAddStation = () => {
    if (!user) {
      setAuthOpen(true);
      return;
    }
    setPinnedPoint(null);
    setPinMode(true);
  };

  const handlePickLocation = (lat: number, lng: number) => {
    setPinnedPoint([lat, lng]);
    setPinMode(false);
    setAddOpen(true);
  };

  // The station currently being routed to, and the geolocation watch handle.
  const destStationRef = useRef<StationSummary | null>(null);
  const watchIdRef = useRef<number | null>(null);

  /** Computes + draws a route from `from` to `station`, updating map + ETA summary. */
  const routeToStation = useCallback(
    async (from: { lat: number; lng: number }, station: StationSummary): Promise<boolean> => {
      if (!station.location) return false;
      try {
        const result = await fetchRoute(from, {
          lat: station.location.lat,
          lng: station.location.lng,
        });
        setOrigin([from.lat, from.lng]);
        setRoute(result.coordinates);
        setRouteSummary({
          stationId: station.id,
          stationName: station.name,
          distanceText: formatDistance(result.distanceMeters),
          durationText: formatDuration(result.durationSeconds),
          durationSeconds: result.durationSeconds,
          trafficAware: result.trafficAware,
        });
        setLiveEtaSeconds(result.durationSeconds);
        destStationRef.current = station;
        return true;
      } catch {
        return false;
      }
    },
    [],
  );

  /**
   * Finds the nearest station reachable within the battery minutes. Queries the backend
   * for stations around the user's current position (a radius scaled to the battery range),
   * so candidates aren't limited to the on-screen nearby list. Prefers stations that are
   * AVAILABLE now; falls back to the nearest reachable station of any status. Returns the
   * on-screen station object when the candidate is already loaded, else the fetched one.
   */
  const findSuggestion = useCallback(
    async (from: { lat: number; lng: number }, minutes: number, excludeId?: string) => {
      const budget = minutes * (1 - RANGE_SAFETY_BUFFER);
      // Max drivable distance ~ budget minutes at AVG_SPEED_KMH; cap the search radius sensibly.
      const radiusKm = Math.min(60, Math.max(RADIUS_KM, (budget / 60) * AVG_SPEED_KMH));
      let pool: StationSummary[] = stations;
      try {
        pool = await api.nearby(from.lat, from.lng, radiusKm, {});
      } catch {
        // Network hiccup: fall back to the already-loaded nearby stations.
        pool = stations;
      }
      const reachable = pool
        .filter((s) => s.location && s.id !== excludeId)
        .map((s) => ({ station: s, etaMinutes: estimateMinutes(from, s.location!) }))
        .filter((c) => c.etaMinutes <= budget)
        .sort((a, b) => a.etaMinutes - b.etaMinutes);

      const availableFirst = reachable.filter(
        (c) => c.station.availabilitySummary === "AVAILABLE" && c.station.availableCount > 0,
      );
      return availableFirst[0] ?? reachable[0] ?? null;
    },
    [stations],
  );

  // Called from the detail sheet's Navigate. Resolves with a summary or null.
  const handleNavigate = (destination: { lat: number; lng: number; name: string; id: string }) =>
    new Promise<{ distanceText: string; durationText: string; trafficAware: boolean } | null>(
      (resolve) => {
        if (!navigator.geolocation) {
          resolve(null);
          return;
        }
        navigator.geolocation.getCurrentPosition(
          async (pos) => {
            const from = { lat: pos.coords.latitude, lng: pos.coords.longitude };
            const station =
              stations.find((s) => s.id === destination.id) ??
              ({
                id: destination.id,
                name: destination.name,
                location: { lat: destination.lat, lng: destination.lng },
                availabilitySummary: "UNKNOWN",
                availableCount: 0,
                totalChargers: 0,
                operator: null,
                area: null,
                distanceMeters: null,
                pricePerKwh: null,
                availabilityUpdatedAt: null,
                dataSource: null,
                confidence: null,
                lastUpdated: null,
              } as StationSummary);
            const ok = await routeToStation(from, station);
            if (!ok) {
              resolve(null);
              return;
            }
            resolve({ distanceText: "", durationText: "", trafficAware: false });
          },
          () => resolve(null),
          { enableHighAccuracy: true, timeout: 10000 },
        );
      },
    );

  const acceptSuggestion = async () => {
    if (!suggestion || !origin) return;
    await routeToStation({ lat: origin[0], lng: origin[1] }, suggestion.station);
    setSuggestion(null);
  };

  /** Clears the active route + map polyline. Keeps battery time unless `keepBattery` is false. */
  const resetRoute = useCallback((keepBattery = true) => {
    if (watchIdRef.current != null && navigator.geolocation) {
      navigator.geolocation.clearWatch(watchIdRef.current);
      watchIdRef.current = null;
    }
    destStationRef.current = null;
    setRoute(null);
    setOrigin(null);
    setRouteSummary(null);
    setSuggestion(null);
    setLiveEtaSeconds(null);
    if (!keepBattery) setBatteryMinutes(null);
  }, []);

  // The Directions card's close button clears everything, battery time included.
  const clearRoute = () => resetRoute(false);

  // Selecting any station (map marker or nearby card) resets the current route/directions
  // but preserves the battery time the user entered.
  const selectStation = useCallback(
    (id: string | null) => {
      resetRoute(true);
      setSelectedId(id);
    },
    [resetRoute],
  );

  // Live ETA countdown: ticks the displayed ETA down while a route is active.
  useEffect(() => {
    if (liveEtaSeconds == null || !routeSummary) return;
    const t = setInterval(() => {
      setLiveEtaSeconds((s) => (s == null ? s : Math.max(0, s - 1)));
    }, 1000);
    return () => clearInterval(t);
  }, [liveEtaSeconds != null, routeSummary?.stationId]);

  // Recompute the real route/ETA when the device actually moves (>60 m) during a trip.
  useEffect(() => {
    if (!routeSummary || !navigator.geolocation) return;
    const id = navigator.geolocation.watchPosition(
      (pos) => {
        const here = { lat: pos.coords.latitude, lng: pos.coords.longitude };
        const station = destStationRef.current;
        if (!station?.location) return;
        // Only recompute if moved meaningfully from the current origin.
        setOrigin((prev) => {
          if (prev) {
            const movedM = estimateMinutes(here, { lat: prev[0], lng: prev[1] }) * (40 / 60) * 1000;
            if (movedM < 60) return prev;
          }
          void routeToStation(here, station);
          return prev;
        });
      },
      () => {},
      { enableHighAccuracy: true, maximumAge: 5000, timeout: 15000 },
    );
    watchIdRef.current = id;
    return () => {
      navigator.geolocation.clearWatch(id);
      watchIdRef.current = null;
    };
  }, [routeSummary?.stationId, routeToStation]);

  // When battery minutes change (or a route starts), check if the current target fits;
  // if not, suggest the nearest reachable station (queried around the user's location).
  useEffect(() => {
    if (batteryMinutes == null || !routeSummary || !origin) {
      setSuggestion(null);
      return;
    }
    const targetMins = routeSummary.durationSeconds / 60;
    const budget = batteryMinutes * (1 - RANGE_SAFETY_BUFFER);
    if (targetMins <= budget) {
      setSuggestion(null); // current destination is reachable
      return;
    }
    let cancelled = false;
    findSuggestion({ lat: origin[0], lng: origin[1] }, batteryMinutes, routeSummary.stationId)
      .then((found) => {
        if (!cancelled) setSuggestion(found);
      })
      .catch(() => {
        if (!cancelled) setSuggestion(null);
      });
    return () => {
      cancelled = true;
    };
  }, [batteryMinutes, routeSummary, origin, findSuggestion]);

  // Fetch the user's current location once they enter a battery range, so nearby
  // cards can show estimated drive time. Reuses the active route origin if present.
  useEffect(() => {
    if (batteryMinutes == null) return;
    if (origin) {
      setUserLoc(origin);
      return;
    }
    if (userLoc || !navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition(
      (pos) => setUserLoc([pos.coords.latitude, pos.coords.longitude]),
      () => {},
      { enableHighAccuracy: true, timeout: 10000 },
    );
  }, [batteryMinutes, origin, userLoc]);

  // Per-station estimated drive time (minutes) + reachability, only when battery range is set.
  const stationEtas = useMemo(() => {
    if (batteryMinutes == null || !userLoc) {
      return new Map<string, { text: string; reachable: boolean }>();
    }
    const from = { lat: userLoc[0], lng: userLoc[1] };
    const budget = batteryMinutes * (1 - RANGE_SAFETY_BUFFER);
    const map = new Map<string, { text: string; reachable: boolean }>();
    for (const s of stations) {
      if (!s.location) continue;
      const mins = estimateMinutes(from, s.location);
      map.set(s.id, {
        text: formatDuration(mins * 60),
        reachable: mins <= budget,
      });
    }
    return map;
  }, [batteryMinutes, userLoc, stations]);

  return (
    <Box className="app-shell">
      <Box sx={{ position: "absolute", inset: 0 }}>
        <StationMap
          center={center}
          stations={stations}
          selectedId={selectedId}
          onSelect={selectStation}
          pinMode={pinMode}
          pinnedPoint={pinnedPoint}
          onPickLocation={handlePickLocation}
          route={route}
          origin={origin}
        />
      </Box>

      {/* Route ETA panel — persists while a route is displayed. Top-right on desktop,
          full-width top strip on mobile so it never overflows. */}
      {routeSummary && !pinMode && (
        <Paper
          elevation={0}
          sx={{
            position: "absolute",
            top: "calc(var(--safe-top) + 62px)",
            right: { xs: 12, sm: 12 },
            left: { xs: 12, sm: "auto" },
            width: { xs: "auto", sm: 300 },
            zIndex: 1250,
            borderRadius: 3,
            overflow: "hidden",
            boxShadow: "0 8px 26px rgba(20,45,30,.2)",
          }}
        >
          {/* Header */}
          <Stack
            direction="row"
            alignItems="center"
            spacing={1}
            sx={{ px: 1.5, py: 1, bgcolor: "rgba(0,255,157,.10)" }}
          >
            <NavigationRoundedIcon sx={{ fontSize: 18, color: "primary.main" }} />
            <Typography sx={{ fontSize: 13, fontWeight: 700, flex: 1 }} color="primary.dark">
              Directions
            </Typography>
            <IconButton
              size="small"
              onClick={clearRoute}
              sx={{ width: 26, height: 26, bgcolor: "rgba(4,6,8,.4)", color: hud.textHigh }}
            >
              <ClearIcon sx={{ fontSize: 16 }} />
            </IconButton>
          </Stack>

          {/* Body */}
          <Box sx={{ px: 1.75, pt: 1.25, pb: 1.75 }}>
            <Typography sx={{ fontSize: 12 }} color="text.secondary" noWrap>
              To {routeSummary.stationName}
            </Typography>
            <Stack direction="row" alignItems="baseline" spacing={0.75} sx={{ mt: 0.5 }}>
              <Typography sx={{ fontSize: 26, fontWeight: 800, lineHeight: 1 }} color="text.primary">
                {formatDuration(liveEtaSeconds ?? routeSummary.durationSeconds)}
              </Typography>
              <Typography sx={{ fontSize: 13 }} color="text.secondary">
                · {routeSummary.distanceText}
              </Typography>
            </Stack>
            <Box sx={{ mt: 1 }}>
              <Chip
                size="small"
                label={routeSummary.trafficAware ? "Live traffic" : "No live traffic"}
                color={routeSummary.trafficAware ? "success" : "default"}
                variant={routeSummary.trafficAware ? "filled" : "outlined"}
                sx={{ height: 22, fontSize: 11 }}
              />
              <Typography sx={{ fontSize: 10.5, mt: 0.5 }} color="text.secondary">
                ETA updates live as you move.
              </Typography>
            </Box>

            {/* Battery range input is a signed-in feature only. */}
            {user && (
              <>
                <Divider sx={{ my: 1.25 }} />
                <Typography sx={{ fontSize: 11.5, fontWeight: 600 }} color="text.secondary">
                  Battery driving range
                </Typography>
                <TextField
                  type="number"
                  size="small"
                  fullWidth
                  placeholder="Minutes left"
                  value={batteryMinutes ?? ""}
                  onChange={(e) => {
                    const v = e.target.value;
                    setBatteryMinutes(v === "" ? null : Math.max(0, Number(v)));
                  }}
                  InputProps={{ endAdornment: <Typography sx={{ fontSize: 12, color: "text.secondary" }}>min</Typography> }}
                  inputProps={{ min: 0, inputMode: "numeric" }}
                  sx={{ mt: 0.5 }}
                />
              </>
            )}

            {/* Suggestion prompt when the current target is out of battery range. */}
            {user && suggestion && (
              <Box
                sx={{
                  mt: 1.25,
                  p: 1.25,
                  borderRadius: 2,
                  bgcolor: "rgba(255,184,0,.12)",
                  border: "1px solid rgba(255,184,0,.4)",
                }}
              >
                <Typography sx={{ fontSize: 11.5, fontWeight: 700 }} color="warning.dark">
                  This station is farther than your battery time
                </Typography>
                <Typography sx={{ fontSize: 12, mt: 0.25 }} color="text.primary">
                  Try <b>{suggestion.station.name}</b> instead — about{" "}
                  {Math.round(suggestion.etaMinutes)} min away, within your{" "}
                  {batteryMinutes} min range.
                </Typography>
                <Button
                  size="small"
                  variant="contained"
                  color="warning"
                  fullWidth
                  onClick={acceptSuggestion}
                  sx={{ mt: 1, borderRadius: 2, textTransform: "none", fontWeight: 700 }}
                >
                  Reroute here
                </Button>
              </Box>
            )}

            {user && batteryMinutes != null && !suggestion && (
              <Typography sx={{ fontSize: 11, mt: 1 }} color="success.main">
                ✓ Destination is within your battery range.
              </Typography>
            )}
          </Box>
        </Paper>
      )}

      {/* Pin-mode instruction banner */}
      {pinMode && (
        <Paper
          elevation={0}
          sx={{
            position: "absolute",
            top: "calc(var(--safe-top) + 10px)",
            left: 12,
            right: 12,
            zIndex: 1400,
            px: 2,
            py: 1.5,
            borderRadius: 3,
            display: "flex",
            alignItems: "center",
            gap: 1,
            boxShadow: "0 6px 22px rgba(20,45,30,.2)",
          }}
        >
          <PlaceRoundedIcon color="primary" />
          <Box sx={{ flex: 1, minWidth: 0 }}>
            <Typography variant="subtitle2" lineHeight={1.2}>
              Tap the map to place the station
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Pick the exact spot of the charger, then add its details.
            </Typography>
          </Box>
          <Button size="small" onClick={() => setPinMode(false)}>
            Cancel
          </Button>
        </Paper>
      )}

      <Box
        sx={{
          position: "absolute",
          top: "calc(var(--safe-top) + 10px)",
          left: 12,
          right: 12,
          zIndex: 1200,
          display: pinMode ? "none" : "block",
        }}
      >
        <Stack direction="row" spacing={1} alignItems="center">
          <Paper
            elevation={0}
            sx={{
              flex: 1,
              display: "flex",
              alignItems: "center",
              px: 2,
              py: 1,
              borderRadius: 999,
              boxShadow: "0 6px 22px rgba(20,45,30,.14)",
            }}
          >
            <SearchIcon sx={{ color: "text.secondary", mr: 1 }} />
            <InputBase
              placeholder="Search IT Park, DC fast…"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && runSearch()}
              sx={{ flex: 1, fontSize: 15 }}
            />
            {searchTerm && (
              <IconButton size="small" onClick={clearSearch}>
                <ClearIcon fontSize="small" />
              </IconButton>
            )}
          </Paper>

          <Paper
            elevation={0}
            sx={{ borderRadius: 999, boxShadow: "0 6px 22px rgba(20,45,30,.14)" }}
          >
            <IconButton
              onClick={() => (user ? setProfileOpen(true) : setAuthOpen(true))}
              color={user ? "primary" : "default"}
            >
              {user ? (
                <Avatar sx={{ width: 28, height: 28, bgcolor: "primary.main", fontSize: 14 }}>
                  {user.name.charAt(0).toUpperCase()}
                </Avatar>
              ) : (
                <PersonIcon />
              )}
            </IconButton>
          </Paper>
        </Stack>

        <Stack direction="row" spacing={1} mt={1} alignItems="center">
          <Chip
            icon={<TuneIcon />}
            label={activeFilterCount ? `Filters · ${activeFilterCount}` : "Filters"}
            color={activeFilterCount ? "primary" : "default"}
            onClick={() => setShowFilters((s) => !s)}
            sx={{
              bgcolor: activeFilterCount ? undefined : "background.paper",
              boxShadow: "0 4px 14px rgba(20,45,30,.12)",
              height: 34,
            }}
          />
          {searching && (
            <Chip
              label={`Results for “${searchTerm}”`}
              onDelete={clearSearch}
              sx={{
                bgcolor: "background.paper",
                boxShadow: "0 4px 14px rgba(20,45,30,.12)",
                height: 34,
              }}
            />
          )}
        </Stack>

        <Collapse in={showFilters}>
          <Paper
            elevation={0}
            sx={{
              mt: 1,
              p: 2.5,
              borderRadius: 2,
              bgcolor: hud.surface1,
              border: "1px solid",
              borderColor: hud.borderMint,
              boxShadow: "0 0 24px rgba(0,255,157,.10)",
              maxWidth: 420,
            }}
          >
            <FilterBar filters={filters} onChange={setFilters} />
          </Paper>
        </Collapse>
      </Box>

      <BottomSheet
        bottomInset={BOTTOM_NAV_HEIGHT}
        onFractionChange={setSheetFraction}
        onSnapChange={(snapIndex) => {
          // Expanding the nearby list above its peek closes the station detail panel so
          // the two never fight for space.
          if (snapIndex > 0 && selectedId) setSelectedId(null);
        }}
      >
        <Stack direction="row" alignItems="baseline" justifyContent="space-between" mb={0.75}>
          <Typography sx={{ fontSize: 15, fontWeight: 700 }}>{VIEW_LABEL[view]}</Typography>
          <Typography variant="caption" color="text.secondary">
            {loading ? "" : `${stations.length} found`}
          </Typography>
        </Stack>

        {loading ? (
          <Stack alignItems="center" py={5}>
            <CircularProgress />
          </Stack>
        ) : stations.length === 0 ? (
          <Box sx={{ textAlign: "center", py: 5, color: "text.secondary" }}>
            <BoltIcon sx={{ fontSize: 40, opacity: 0.4 }} />
            <Typography sx={{ mt: 1 }}>
              {view === "favorites" && !user
                ? "Sign in to save favorite stations."
                : "No stations match. Try widening filters."}
            </Typography>
          </Box>
        ) : (
          <Stack spacing={0.75}>
            {stations.map((s) => {
              const eta = stationEtas.get(s.id);
              return (
                <StationCard
                  key={s.id}
                  station={s}
                  favorite={favoriteIds.has(s.id)}
                  selected={s.id === selectedId}
                  onClick={() => selectStation(s.id)}
                  etaText={eta?.text ?? null}
                  reachable={eta?.reachable ?? true}
                />
              );
            })}
          </Stack>
        )}
      </BottomSheet>

      <Paper
        elevation={0}
        sx={{
          position: "absolute",
          left: 0,
          right: 0,
          bottom: 0,
          zIndex: 1300,
          borderTopLeftRadius: 28,
          borderTopRightRadius: 28,
          boxShadow: "0 -8px 30px rgba(20,45,30,.14)",
          pb: "var(--safe-bottom)",
        }}
      >
        <BottomNavigation
          showLabels
          value={view}
          onChange={(_, v) => {
            setSearching(false);
            setSearchTerm("");
            setView(v as View);
          }}
          sx={{
            height: BOTTOM_NAV_HEIGHT,
            bgcolor: "transparent",
            // Always show compact labels under each icon (not just the selected one).
            "& .MuiBottomNavigationAction-label": {
              fontSize: 11,
              fontWeight: 600,
              opacity: 1,
              mt: 0.25,
              "&.Mui-selected": { fontSize: 11 },
            },
          }}
        >
          <BottomNavigationAction label="Nearby" value="nearby" icon={<ExploreIcon />} />
          <BottomNavigationAction label="Cheapest" value="cheapest" icon={<SavingsIcon />} />
          <BottomNavigationAction label="Available" value="available" icon={<BoltIcon />} />
          <BottomNavigationAction
            label="Saved"
            value="favorites"
            icon={
              <Badge badgeContent={favoriteIds.size} color="primary" max={9}>
                <FavoriteIcon />
              </Badge>
            }
          />
        </BottomNavigation>
      </Paper>

      {/* Add-station FAB: rides just above the top edge of the nearby-charger sheet, so it
          moves with the sheet as it drags up/down and never overlaps the station detail
          panel (which is on the right and stops above the sheet). Hidden while pin-placing.
          Only shown to signed-in users. */}
      {!pinMode && user && (
        <Fab
          color="primary"
          variant="extended"
          onClick={() => {
            setSelectedId(null); // close the station dialog if open
            startAddStation();
          }}
          sx={{
            position: "absolute",
            // Right edge normally; shift left of the detail panel (400px + gutter) when it's
            // open so the two never overlap.
            right: selectedId ? { xs: 16, md: 428 } : 16,
            // Sit ~16px above the sheet's top edge (sheet height = sheetFraction of shell).
            bottom: `calc(${sheetFraction * 100}% + 16px)`,
            zIndex: 1370,
            transition: "bottom .2s ease, right .25s ease",
          }}
        >
          <AddRoundedIcon sx={{ mr: 1 }} />
          Add station
        </Fab>
      )}

      <StationDetailSheet
        stationId={selectedId}
        onClose={() => setSelectedId(null)}
        isFavorite={selectedId ? favoriteIds.has(selectedId) : false}
        onToggleFavorite={toggleFavorite}
        onRequireLogin={() => {
          // Close the station sheet first so the auth dialog isn't stacked under the
          // sheet's modal layer (which would make it impossible to dismiss).
          setSelectedId(null);
          setAuthOpen(true);
        }}
        onNavigate={handleNavigate}
      />
      <AuthDialog open={authOpen} onClose={() => setAuthOpen(false)} />
      <AddStationDialog
        open={addOpen}
        onClose={() => setAddOpen(false)}
        onSubmitted={load}
        defaultCenter={CEBU}
        initialLocation={pinnedPoint}
      />
      <ProfileSheet open={profileOpen} onClose={() => setProfileOpen(false)} />
    </Box>
  );
}
