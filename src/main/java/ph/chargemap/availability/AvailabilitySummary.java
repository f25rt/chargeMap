package ph.chargemap.availability;

/**
 * Station-level availability rollup shown on the map (product spec section 7).
 * Distinct from per-charger {@code ChargerStatus}: a station is summarized as one of
 * three states for marker coloring.
 */
public enum AvailabilitySummary {
    AVAILABLE,
    OCCUPIED,
    UNKNOWN
}
