package ph.chargemap.station.imports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Runs the OSM import once at startup, ONLY when {@code chargemap.import.osm.enabled=true}.
 * This keeps the import an explicit, opt-in operation: set the flag + bounding box (via
 * env vars), start the app to seed real stations, then unset the flag for normal runs.
 *
 * <p>Bounding box is configured via {@code chargemap.import.osm.bbox.*}; the defaults
 * cover the Philippines. Narrow it to Metro Cebu for a focused first launch.
 */
@Component
@ConditionalOnProperty(name = "chargemap.import.osm.enabled", havingValue = "true")
public class OsmImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OsmImportRunner.class);

    private final OsmImportService importService;
    private final OsmImportProperties props;

    public OsmImportRunner(OsmImportService importService, OsmImportProperties props) {
        this.importService = importService;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            String file = props.getFile();
            if (file != null && !file.isBlank()) {
                // File mode: import a pre-fetched Overpass JSON (works behind a proxy that
                // breaks the backend's direct HTTPS to Overpass).
                var path = java.nio.file.Path.of(file);
                log.info("Starting OSM import from file {}", path.toAbsolutePath());
                var result = importService.importFromFile(path);
                log.info("OSM import finished: {}", result);
                return;
            }
            var bbox = props.getBbox();
            log.info("Starting OSM charging-station import for bbox S={} W={} N={} E={}",
                    bbox.getSouth(), bbox.getWest(), bbox.getNorth(), bbox.getEast());
            var result = importService.importBoundingBox(
                    bbox.getSouth(), bbox.getWest(), bbox.getNorth(), bbox.getEast());
            log.info("OSM import finished: {}", result);
        } catch (Exception e) {
            // Never let an import failure crash the app; log and continue serving.
            log.error("OSM import failed: {}", e.getMessage(), e);
        }
    }
}
