package ph.chargemap.station.imports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ph.chargemap.station.Station;
import ph.chargemap.station.StationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Imports real EV charging stations from OpenStreetMap into the {@code stations}
 * collection. Idempotent: re-running updates existing OSM-sourced stations (matched by
 * {@code sourceRef}) instead of creating duplicates, and never overwrites community data
 * such as availability, prices, reviews, or likes.
 *
 * <p>Triggered on demand (see {@link OsmImportRunner}); it does not run automatically on
 * every startup.
 */
@Service
public class OsmImportService {

    private static final Logger log = LoggerFactory.getLogger(OsmImportService.class);

    private final OverpassClient overpass;
    private final OsmStationMapper mapper;
    private final StationRepository repository;

    public OsmImportService(OverpassClient overpass, OsmStationMapper mapper,
                            StationRepository repository) {
        this.overpass = overpass;
        this.mapper = mapper;
        this.repository = repository;
    }

    /** Result summary of an import run. */
    public record ImportResult(int fetched, int created, int updated, int skipped) {
        @Override
        public String toString() {
            return "fetched=" + fetched + ", created=" + created
                    + ", updated=" + updated + ", skipped=" + skipped;
        }
    }

    /**
     * Imports from a pre-fetched Overpass JSON file. Useful when the backend cannot reach
     * Overpass directly (e.g. behind a TLS-inspecting corporate proxy): fetch the JSON with
     * a tool that trusts the proxy CA, save it, and point this at the file.
     */
    public ImportResult importFromFile(java.nio.file.Path file) {
        String body;
        try {
            body = java.nio.file.Files.readString(file);
        } catch (java.io.IOException e) {
            log.error("OSM import: could not read file {}: {}", file, e.getMessage());
            return new ImportResult(0, 0, 0, 0);
        }
        List<OverpassClient.OverpassElement> elements = overpass.parseBody(body);
        return upsert(elements);
    }

    /**
     * Fetches charging stations in the bounding box and upserts them. Coordinates are
     * decimal degrees (south &lt; north, west &lt; east).
     */
    public ImportResult importBoundingBox(double south, double west, double north, double east) {
        List<OverpassClient.OverpassElement> elements =
                overpass.fetchChargingStations(south, west, north, east);
        return upsert(elements);
    }

    /** Maps, dedups, and upserts a batch of Overpass elements. */
    private ImportResult upsert(List<OverpassClient.OverpassElement> elements) {

        // Map to fresh Station objects, keyed by sourceRef (dedup within the batch too).
        Map<String, Station> freshByRef = new java.util.LinkedHashMap<>();
        int skipped = 0;
        for (OverpassClient.OverpassElement el : elements) {
            Station fresh = mapper.toStation(el);
            if (fresh == null || fresh.getSourceRef() == null) {
                skipped++;
                continue;
            }
            freshByRef.put(fresh.getSourceRef(), fresh);
        }

        if (freshByRef.isEmpty()) {
            log.info("OSM import: nothing to import (fetched {}, skipped {})", elements.size(), skipped);
            return new ImportResult(elements.size(), 0, 0, skipped);
        }

        // Load already-imported stations for these refs in one query.
        Map<String, Station> existingByRef = repository.findBySourceRefIn(freshByRef.keySet())
                .stream()
                .collect(Collectors.toMap(Station::getSourceRef, s -> s, (a, b) -> a));

        List<Station> toSave = new ArrayList<>();
        int created = 0;
        int updated = 0;
        for (Map.Entry<String, Station> e : freshByRef.entrySet()) {
            Station existing = existingByRef.get(e.getKey());
            if (existing == null) {
                toSave.add(e.getValue());
                created++;
            } else {
                mapper.applyImportableFields(existing, e.getValue());
                toSave.add(existing);
                updated++;
            }
        }

        repository.saveAll(toSave);
        ImportResult result = new ImportResult(elements.size(), created, updated, skipped);
        log.info("OSM import complete: {}", result);
        return result;
    }
}
