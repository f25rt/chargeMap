package ph.chargemap.common.geo;

/**
 * Client-friendly coordinate pair exposed in DTOs as {@code {lat, lng}}.
 * Internally stations store GeoJSON {@code [lng, lat]}; {@link GeoUtil} handles
 * the conversion in one place.
 */
public record GeoPoint(double lat, double lng) {
}
