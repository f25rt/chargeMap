package ph.chargemap.telemetry;

import java.time.Instant;
import java.util.List;

/**
 * DTOs for SIMULATED station/grid hardware telemetry.
 *
 * <p><b>Important:</b> ChargeMap has no live charger-hardware feed (that requires an OCPP
 * integration or OCPI roaming agreement with each operator, which we don't have). Every
 * value here is synthesized for demonstration and is flagged {@code simulated = true}. The
 * UI must label it as simulated; it must never be presented as real live hardware data.
 */
public final class SimTelemetryDtos {

    private SimTelemetryDtos() {
    }

    /** Simulated live state of a single charging bay. */
    public record BayTelemetryDto(
            int bayNumber,
            String connectorType,
            double powerKw,
            String state,          // AVAILABLE | DISPENSING | HOLD | CALIBRATING
            Integer socPercent,    // present only while DISPENSING
            Double deliveringKw,   // present only while DISPENSING
            Integer etaMinutes     // present only while DISPENSING
    ) {
    }

    /** Simulated per-station live telemetry (bays + rollups). Always {@code simulated=true}. */
    public record StationTelemetryDto(
            boolean simulated,
            String stationId,
            List<BayTelemetryDto> bays,
            double totalDeliveringKw,
            int baysDispensing,
            Instant asOf
    ) {
    }

    /** A single simulated IoT hardware-stream log line for the admin console. */
    public record IotLogDto(
            Instant timestamp,
            String source,   // e.g. HUB_04 // BAY_03
            String level,    // INFO | WARN | ERROR
            String message
    ) {
    }

    /** Simulated grid-level telemetry for the admin Telemetry Hub. Always {@code simulated=true}. */
    public record GridTelemetryDto(
            boolean simulated,
            double throughputMw,
            double capacityPercent,
            double gridFrequencyHz,
            double thermistorC,
            int rfidLatencyMs,
            List<IotLogDto> iotStream,
            Instant asOf
    ) {
    }
}
