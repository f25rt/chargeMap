package ph.chargemap.telemetry;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import ph.chargemap.charger.Charger;
import ph.chargemap.charger.ChargerStatus;
import ph.chargemap.common.error.BadRequestException;
import ph.chargemap.common.error.NotFoundException;
import ph.chargemap.station.Station;
import ph.chargemap.station.StationRepository;
import ph.chargemap.telemetry.SimTelemetryDtos.BayTelemetryDto;
import ph.chargemap.telemetry.SimTelemetryDtos.GridTelemetryDto;
import ph.chargemap.telemetry.SimTelemetryDtos.IotLogDto;
import ph.chargemap.telemetry.SimTelemetryDtos.StationTelemetryDto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates SIMULATED station and grid telemetry for the HUD demo panels.
 *
 * <p>There is no real charger-hardware feed available (no OCPP/OCPI integration), so this
 * synthesizes plausible values. Output is time-seeded so it changes on each poll, giving a
 * "live" feel, but it is NOT real. Every response is flagged {@code simulated = true}, and
 * callers must label it as simulated in the UI.
 */
@Service
public class SimTelemetryService {

    private final StationRepository stationRepository;

    public SimTelemetryService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    /** Simulated live bay telemetry for one station, derived from its real charger list. */
    public StationTelemetryDto stationTelemetry(String stationId) {
        if (stationId == null || !ObjectId.isValid(stationId)) {
            throw new BadRequestException("Invalid station id");
        }
        Station station = stationRepository.findById(new ObjectId(stationId))
                .orElseThrow(() -> NotFoundException.station(stationId));

        // Seed varies per-minute so repeated polls animate but stay stable within a minute.
        long minuteSeed = System.currentTimeMillis() / 60_000;
        Random rnd = new Random(station.getId().hashCode() * 31L + minuteSeed);

        List<BayTelemetryDto> bays = new ArrayList<>();
        double totalDelivering = 0;
        int dispensing = 0;
        int i = 0;
        for (Charger c : station.getChargers()) {
            i++;
            String state;
            Integer soc = null;
            Double deliveringKw = null;
            Integer eta = null;
            // Map the real charger status to a plausible simulated live state.
            if (c.getStatus() == ChargerStatus.OCCUPIED) {
                state = "DISPENSING";
            } else if (c.getStatus() == ChargerStatus.AVAILABLE) {
                state = rnd.nextInt(4) == 0 ? "DISPENSING" : "AVAILABLE";
            } else if (c.getStatus() == ChargerStatus.BROKEN) {
                state = "CALIBRATING";
            } else {
                state = rnd.nextInt(3) == 0 ? "HOLD" : "AVAILABLE";
            }
            if ("DISPENSING".equals(state)) {
                soc = 20 + rnd.nextInt(75); // 20–94%
                deliveringKw = Math.round(c.getPowerKw() * (0.55 + rnd.nextDouble() * 0.4) * 10) / 10.0;
                eta = Math.max(2, (100 - soc) / 3 + rnd.nextInt(8));
                totalDelivering += deliveringKw;
                dispensing++;
            }
            bays.add(new BayTelemetryDto(i, c.getConnectorType().name(), c.getPowerKw(),
                    state, soc, deliveringKw, eta));
        }

        return new StationTelemetryDto(true, station.getId().toHexString(), bays,
                Math.round(totalDelivering * 10) / 10.0, dispensing, Instant.now());
    }

    /** Simulated grid-level telemetry + IoT stream for the admin Telemetry Hub. */
    public GridTelemetryDto gridTelemetry() {
        long secondSeed = System.currentTimeMillis() / 10_000; // refresh every 10s
        Random rnd = new Random(secondSeed);

        double throughput = Math.round((3.5 + rnd.nextDouble() * 3.0) * 100) / 100.0; // 3.5–6.5 MW
        double capacity = Math.round((60 + rnd.nextDouble() * 30) * 10) / 10.0; // 60–90%
        double freq = Math.round((59.95 + rnd.nextDouble() * 0.1) * 100) / 100.0;
        double thermistor = Math.round((38 + rnd.nextDouble() * 8) * 10) / 10.0;
        int rfid = 12 + rnd.nextInt(12);

        String[] sources = {"HUB_04 // BAY_03", "RFID_SVC", "PWR_GRID", "TELEM_DAEMON", "THERMAL", "TARIFF"};
        String[] msgs = {
                "CCS2 handshake successful. DIN 70121 ISO 15118 ready.",
                "Card UID authorized. Balance confirmed.",
                "Bay power ramped to target max.",
                "Ping sweep: endpoints responded.",
                "Heat dissipation duty cycle adjusted.",
                "Dynamic rate synced for Zone Cebu-Central.",
        };
        String[] levels = {"INFO", "INFO", "INFO", "WARN", "INFO", "INFO"};
        List<IotLogDto> stream = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = 0; i < 6; i++) {
            int idx = rnd.nextInt(sources.length);
            stream.add(new IotLogDto(now.minusSeconds(i * 7L), sources[idx], levels[idx], msgs[idx]));
        }

        return new GridTelemetryDto(true, throughput, capacity, freq, thermistor, rfid, stream, now);
    }
}
