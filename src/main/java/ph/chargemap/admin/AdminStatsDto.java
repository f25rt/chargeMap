package ph.chargemap.admin;

/** Aggregate system metrics for the admin dashboard (product spec section 38). */
public record AdminStatsDto(
        long totalStations,
        long totalChargers,
        long availableChargers,
        long totalUsers,
        long totalReports,
        long staleStations
) {
}
