import { MapContainer, TileLayer, Marker, Polyline, useMap, useMapEvents } from "react-leaflet";
import L from "leaflet";
import { useEffect, useRef } from "react";
import type { StationSummary } from "../api/types";
import { availabilityHex } from "../theme";

// Fix Leaflet's default icon paths (harmless; we use custom divIcons).
delete (L.Icon.Default.prototype as unknown as { _getIconUrl?: unknown })._getIconUrl;

/**
 * A station marker: a rounded pill with an EV-station glyph (or the branch's icon when
 * one is set) + price, colored by availability. Enlarges when selected.
 */
function priceIcon(station: StationSummary, selected: boolean): L.DivIcon {
  const color = availabilityHex(station.availabilitySummary);
  const price = station.pricePerKwh != null ? `₱${station.pricePerKwh}` : "—";
  const scale = selected ? 1.15 : 1;
  const ring = selected
    ? "box-shadow:0 0 0 4px rgba(27,143,77,.35),0 2px 6px rgba(0,0,0,.4);"
    : "box-shadow:0 2px 6px rgba(0,0,0,.4);";
  // Branch icon (custom image) if provided, else a default EV-charger glyph.
  const branchIcon = (station as StationSummary & { branchIconUrl?: string | null }).branchIconUrl;
  const glyph = branchIcon
    ? `<img src="${branchIcon}" style="width:14px;height:14px;border-radius:3px;object-fit:cover;margin-right:3px;" />`
    : `<svg width="12" height="12" viewBox="0 0 24 24" fill="#fff" style="margin-right:3px;flex-shrink:0;"><path d="M7 2h7a2 2 0 0 1 2 2v7h1a2 2 0 0 1 2 2v4a1.5 1.5 0 0 0 3 0V9l-2.5-2.5 1-1L22 8.2V17a3.5 3.5 0 0 1-7 0v-4h-1v9H5V4a2 2 0 0 1 2-2Zm0 2v6h7V4H7Z"/></svg>`;
  return L.divIcon({
    className: "chargemap-marker",
    html: `<div style="
      transform:scale(${scale});transform-origin:center bottom;
      display:flex;align-items:center;
      background:${color};color:#fff;font:700 12px Roboto,sans-serif;
      padding:4px 9px;border-radius:14px;white-space:nowrap;
      border:2px solid #fff;${ring}">${glyph}${price}</div>`,
    iconSize: [64, 24],
    iconAnchor: [32, 12],
  });
}

function Recenter({ center }: { center: [number, number] }) {
  const map = useMap();
  useEffect(() => {
    map.setView(center, map.getZoom(), { animate: true });
  }, [center, map]);
  return null;
}

/** A pin drop marker for the location being placed while adding a station. */
function pinIcon(): L.DivIcon {
  return L.divIcon({
    className: "chargemap-pin",
    html: `<div style="
      width:26px;height:26px;transform:translateY(-6px);
      background:#1f9d57;border:3px solid #fff;border-radius:50% 50% 50% 0;
      transform:rotate(-45deg) translateY(-2px);
      box-shadow:0 3px 8px rgba(0,0,0,.4);"></div>`,
    iconSize: [26, 26],
    iconAnchor: [13, 26],
  });
}

/**
 * In pin mode, captures the location when the user releases a click on the map
 * (mouseup), ignoring releases that were the end of a pan/drag.
 */
function PinPicker({ onPick }: { onPick: (lat: number, lng: number) => void }) {
  const draggedRef = useRef(false);
  useMapEvents({
    dragstart: () => {
      draggedRef.current = true;
    },
    mousedown: () => {
      draggedRef.current = false;
    },
    mouseup: (e) => {
      if (draggedRef.current) return; // was a pan, not a pin
      onPick(e.latlng.lat, e.latlng.lng);
    },
  });
  return null;
}

/**
 * Fits the map to the route when one is present. We only fit to the route's own bounds
 * (origin → destination) with generous padding; nearby station markers keep rendering
 * and remain visible around the route rather than being zoomed out of view.
 */
function FitRoute({ route }: { route: [number, number][] | null }) {
  const map = useMap();
  useEffect(() => {
    if (route && route.length > 1) {
      const bounds = L.latLngBounds(route as L.LatLngExpression[]);
      map.fitBounds(bounds, { padding: [60, 60], maxZoom: 14 });
    }
  }, [route, map]);
  return null;
}

/** Pans (without zoom change) to the selected station so it stays in view. */
function PanToSelected({
  stations,
  selectedId,
}: {
  stations: StationSummary[];
  selectedId: string | null;
}) {
  const map = useMap();
  useEffect(() => {
    if (!selectedId) return;
    const s = stations.find((x) => x.id === selectedId);
    if (s?.location) {
      map.panTo([s.location.lat, s.location.lng], { animate: true });
    }
  }, [selectedId, stations, map]);
  return null;
}

interface Props {
  center: [number, number];
  stations: StationSummary[];
  selectedId: string | null;
  onSelect: (id: string) => void;
  pinMode?: boolean;
  pinnedPoint?: [number, number] | null;
  onPickLocation?: (lat: number, lng: number) => void;
  /** In-app route polyline [lat,lng][] from the user's location to a station. */
  route?: [number, number][] | null;
  /** The user's current location, shown as the route origin. */
  origin?: [number, number] | null;
}

/** A car marker for the user's current location (route origin). */
function originIcon(): L.DivIcon {
  return L.divIcon({
    className: "chargemap-origin",
    html: `<div style="
      width:34px;height:34px;border-radius:50%;
      background:#2563eb;border:3px solid #fff;
      display:flex;align-items:center;justify-content:center;
      box-shadow:0 0 0 4px rgba(37,99,235,.25),0 2px 6px rgba(0,0,0,.35);">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="#fff"><path d="M18.92 6.01C18.72 5.42 18.16 5 17.5 5h-11c-.66 0-1.21.42-1.42 1.01L3 12v8a1 1 0 0 0 1 1h1a1 1 0 0 0 1-1v-1h12v1a1 1 0 0 0 1 1h1a1 1 0 0 0 1-1v-8l-2.08-5.99ZM6.5 16a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3Zm11 0a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3ZM5 11l1.5-4.5h11L19 11H5Z"/></svg>
      </div>`,
    iconSize: [34, 34],
    iconAnchor: [17, 17],
  });
}

export default function StationMap({
  center,
  stations,
  selectedId,
  onSelect,
  pinMode = false,
  pinnedPoint = null,
  onPickLocation,
  route = null,
  origin = null,
}: Props) {
  return (
    <MapContainer
      center={center}
      zoom={13}
      zoomControl={false}
      className={pinMode ? "chargemap-pin-cursor" : undefined}
      style={{ height: "100%", width: "100%" }}
    >
      <TileLayer
        attribution='Map & charging-station data &copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap</a> contributors (ODbL)'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <Recenter center={center} />
      <PanToSelected stations={stations} selectedId={selectedId} />
      <FitRoute route={route} />
      {pinMode && onPickLocation && <PinPicker onPick={onPickLocation} />}
      {pinnedPoint && <Marker position={pinnedPoint} icon={pinIcon()} />}
      {route && route.length > 1 && (
        <>
          <Polyline positions={route} pathOptions={{ color: "#1f9d57", weight: 5, opacity: 0.85 }} />
          {origin && <Marker position={origin} icon={originIcon()} />}
        </>
      )}
      {stations
        .filter((s) => s.location)
        .map((s) => (
          <Marker
            key={s.id}
            position={[s.location!.lat, s.location!.lng]}
            icon={priceIcon(s, s.id === selectedId)}
            zIndexOffset={s.id === selectedId ? 1000 : 0}
            eventHandlers={{ click: () => onSelect(s.id) }}
          />
        ))}
    </MapContainer>
  );
}
