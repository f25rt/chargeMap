// Driving-route lookup for the in-app Navigate feature.
//
// Prefers the Mapbox Directions API with the `driving-traffic` profile (live,
// traffic-aware ETAs and least-traffic routing) when a public token is configured
// via VITE_MAPBOX_TOKEN. Falls back to the free public OSRM demo server (no key,
// free-flow ETA, no live traffic) when no token is present or Mapbox fails.

export interface RouteResult {
  /** Route polyline as [lat, lng] points, ready for Leaflet. */
  coordinates: [number, number][];
  /** Total distance in meters. */
  distanceMeters: number;
  /** Estimated driving duration in seconds. */
  durationSeconds: number;
  /** True when the duration reflects live traffic (Mapbox driving-traffic). */
  trafficAware: boolean;
}

const MAPBOX_TOKEN = import.meta.env.VITE_MAPBOX_TOKEN as string | undefined;
const OSRM_BASE = "https://router.project-osrm.org/route/v1/driving";
const MAPBOX_BASE = "https://api.mapbox.com/directions/v5/mapbox/driving-traffic";

export type Pt = { lat: number; lng: number };

async function fetchMapbox(origin: Pt, destination: Pt): Promise<RouteResult> {
  const coords = `${origin.lng},${origin.lat};${destination.lng},${destination.lat}`;
  const url =
    `${MAPBOX_BASE}/${coords}?geometries=geojson&overview=full&` +
    `access_token=${MAPBOX_TOKEN}`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Mapbox routing failed (${res.status})`);
  const data = await res.json();
  if (data.code !== "Ok" || !data.routes?.length) throw new Error("No route found");
  const route = data.routes[0];
  const coordinates: [number, number][] = route.geometry.coordinates.map(
    (c: [number, number]) => [c[1], c[0]],
  );
  return {
    coordinates,
    distanceMeters: route.distance,
    durationSeconds: route.duration,
    trafficAware: true,
  };
}

async function fetchOsrm(origin: Pt, destination: Pt): Promise<RouteResult> {
  const coords = `${origin.lng},${origin.lat};${destination.lng},${destination.lat}`;
  const url = `${OSRM_BASE}/${coords}?overview=full&geometries=geojson`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Routing failed (${res.status})`);
  const data = await res.json();
  if (data.code !== "Ok" || !data.routes?.length) throw new Error("No route found");
  const route = data.routes[0];
  const coordinates: [number, number][] = route.geometry.coordinates.map(
    (c: [number, number]) => [c[1], c[0]],
  );
  return {
    coordinates,
    distanceMeters: route.distance,
    durationSeconds: route.duration,
    trafficAware: false,
  };
}

/**
 * Fetches a driving route from origin to destination. Uses Mapbox (traffic-aware)
 * when a token is set, otherwise OSRM; if Mapbox errors, falls back to OSRM.
 */
export async function fetchRoute(origin: Pt, destination: Pt): Promise<RouteResult> {
  if (MAPBOX_TOKEN) {
    try {
      return await fetchMapbox(origin, destination);
    } catch {
      // fall through to OSRM
    }
  }
  return fetchOsrm(origin, destination);
}

/** Formats a duration in seconds as a friendly "1 h 12 min" / "8 min" string. */
export function formatDuration(seconds: number): string {
  const mins = Math.round(seconds / 60);
  if (mins < 60) return `${mins} min`;
  const h = Math.floor(mins / 60);
  const m = mins % 60;
  return m > 0 ? `${h} h ${m} min` : `${h} h`;
}

/** Formats distance in meters as "2.4 km" / "850 m". */
export function formatDistance(meters: number): string {
  if (meters < 1000) return `${Math.round(meters)} m`;
  return `${(meters / 1000).toFixed(1)} km`;
}

/** Average city driving speed (km/h) used to convert battery "minutes" ↔ reachable distance. */
export const AVG_SPEED_KMH = 40;

/** Safety buffer: only treat a station as reachable if it needs ≤ (1 - buffer) of range. */
export const RANGE_SAFETY_BUFFER = 0.15;

/** Great-circle distance in meters between two points (Haversine). */
export function haversineMeters(a: Pt, b: Pt): number {
  const R = 6371000;
  const toRad = (d: number) => (d * Math.PI) / 180;
  const dLat = toRad(b.lat - a.lat);
  const dLng = toRad(b.lng - a.lng);
  const lat1 = toRad(a.lat);
  const lat2 = toRad(b.lat);
  const h =
    Math.sin(dLat / 2) ** 2 + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(h));
}

/** Rough straight-line driving-time estimate in minutes (proxy for ranking candidates). */
export function estimateMinutes(a: Pt, b: Pt): number {
  const km = haversineMeters(a, b) / 1000;
  return (km / AVG_SPEED_KMH) * 60;
}
