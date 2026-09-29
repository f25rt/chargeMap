package ph.chargemap.station.imports;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the OSM importer. Bound from {@code chargemap.import.osm.*}.
 * Defaults cover the whole Philippines; override the bounding box to focus on a region.
 */
@Component
@ConfigurationProperties(prefix = "chargemap.import.osm")
public class OsmImportProperties {

    /** Whether the import runs on startup. Wired via {@code @ConditionalOnProperty} on the runner. */
    private boolean enabled = false;

    /**
     * Optional path to a pre-fetched Overpass JSON file. When set, the importer reads this
     * file instead of calling the Overpass API — useful behind a TLS-inspecting proxy where
     * the backend's direct HTTPS to Overpass fails. Empty = live API mode.
     */
    private String file;

    private Bbox bbox = new Bbox();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public Bbox getBbox() {
        return bbox;
    }

    public void setBbox(Bbox bbox) {
        this.bbox = bbox;
    }

    /** Bounding box in decimal degrees. Defaults span the Philippines archipelago. */
    public static class Bbox {
        private double south = 4.5;
        private double west = 116.0;
        private double north = 21.5;
        private double east = 127.0;

        public double getSouth() {
            return south;
        }

        public void setSouth(double south) {
            this.south = south;
        }

        public double getWest() {
            return west;
        }

        public void setWest(double west) {
            this.west = west;
        }

        public double getNorth() {
            return north;
        }

        public void setNorth(double north) {
            this.north = north;
        }

        public double getEast() {
            return east;
        }

        public void setEast(double east) {
            this.east = east;
        }
    }
}
