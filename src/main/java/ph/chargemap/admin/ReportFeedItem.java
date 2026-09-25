package ph.chargemap.admin;

import ph.chargemap.charger.ChargerStatus;

import java.time.Instant;

/** A recent report enriched with the station name, for the admin moderation feed. */
public record ReportFeedItem(
        String reportId,
        String stationId,
        String stationName,
        String chargerId,
        ChargerStatus status,
        Instant createdAt
) {
}
