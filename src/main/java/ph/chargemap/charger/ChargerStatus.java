package ph.chargemap.charger;

/** Per-charger status reported/derived (product spec sections 7, 20). */
public enum ChargerStatus {
    AVAILABLE,
    OCCUPIED,
    BROKEN,
    CLOSED,
    UNKNOWN
}
