package ph.chargemap.report;

import ph.chargemap.availability.AvailabilitySummary;

import java.time.Instant;

/** Confirmation returned after a report is accepted (Requirement 8.5). */
public record ReportResponse(
        String message,
        String reportId,
        AvailabilitySummary availabilitySummary,
        int availableCount,
        int totalChargers,
        Instant lastUpdated
) {
}
