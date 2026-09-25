import { MapContainer, TileLayer, Marker, Polyline, useMap, useMapEvents } from "react-leaflet";
import L from "leaflet";
import { useEffect, useRef } from "react";
import type { StationSummary } from "../api/types";
import { availabilityHex } from "../theme";

// Fix Leaflet's default icon paths (harmless; we use custom divIcons).
delete (L.Icon.Default.prototype as unknown as { _getIconUrl?: unknown })._getIconUrl;

/** A pill marker showing price + availability color; enlarges when selected. */
function priceIcon(station: StationSummary, selected: boolean): L.DivIcon {
  const color = availabilityHex(station.availabilitySummary);
  const price = station.pricePerKwh != null ? `₱${station.pricePerKwh}` : "—";
  const scale = selected ? 1.15 : 1;
  const ring = selected ? "box-shadow:0 0 0 4px rgba(27,143,77,.35),0 2px 6px rgba(0,0,0,.4);" : "box-shadow:0 2px 6px rgba(0,0,0,.4);";
  return L.divIcon({
    className: "chargemap-marker",
    html: `<div style="
      transform:scale(${scale});transform-origin:center bottom;
      background:${color};color:#fff;font:700 12px Roboto,sans-serif;
      padding:4px 9px;border-radius:14px;white-space:nowrap;
      border:2px solid #fff;${ring}">${price}</div>`,
    iconSize: [50, 24],
    iconAnchor: [25, 12],
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

/** Fits the map to the route bounds when a route is present. */
function FitRoute({ route }: { route: [number, number][] | null }) {
  const map = useMap();
  useEffect(() => {
    if (route && route.length > 1) {
      map.fitBounds(route as L.LatLngBoundsExpression, { padding: [40, 40] });
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

/** A blue dot marker for the user's current location (route origin). */
function originIcon(): L.DivIcon {
  return L.divIcon({
    className: "chargemap-origin",
    html: `<div style="
      width:16px;height:16px;border-radius:50%;
      background:#2563eb;border:3px solid #fff;
      box-shadow:0 0 0 4px rgba(37,99,235,.3);"></div>`,
    iconSize: [16, 16],
    iconAnchor: [8, 8],
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
        attribution='&copy; OpenStreetMap'
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
