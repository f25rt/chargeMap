package ph.chargemap.station.imports;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Component;
import ph.chargemap.availability.AvailabilitySummary;
import ph.chargemap.charger.Charger;
import ph.chargemap.charger.ChargerStatus;
import ph.chargemap.charger.ChargerType;
import ph.chargemap.charger.ConnectorType;
import ph.chargemap.station.Confidence;
import ph.chargemap.station.DataSource;
import ph.chargemap.station.Station;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Maps an OpenStreetMap {@code amenity=charging_station} element to a {@link Station}.
 *
 * <p>OSM tagging is inconsistent, so this is best-effort: it extracts connectors from
 * {@code socket:*} tags, power from {@code :output} tags or {@code maxpower}, and falls
 * back to sensible defaults. Imported stations are marked {@link DataSource#OPEN_DATASET}
 * with {@link Confidence#LOW} and {@link AvailabilitySummary#UNKNOWN} availability — the
 * community layer refines them over time.
 */
@Component
public class OsmStationMapper {

    /**
     * Maps OSM {@code socket:<key>} suffixes to our connector enum. Keys not listed are ignored.
     */
    private static final Map<String, ConnectorType> SOCKET_CONNECTORS = Map.of(
            "type2", ConnectorType.TYPE2,
            "type2_combo", ConnectorType.CCS2,
            "type2_cable", ConnectorType.TYPE2,
            "ccs", ConnectorType.CCS2,
            "chademo", ConnectorType.CHADEMO,
            "gb_dc", ConnectorType.GBT,
            "gb_ac", ConnectorType.GBT,
            "nacs", ConnectorType.NACS,
            "tesla_supercharger", ConnectorType.NACS
    );

    /**
     * Builds a Station from an OSM element, or returns {@code null} if the element is
     * unusable (no coordinate). {@code createdAt}/{@code lastUpdated} are set to now.
     */
    public Station toStation(OverpassClient.OverpassElement el) {
        if (el == null || (el.lat() == 0 && el.lon() == 0)) {
            return null;
        }
        Map<String, String> tags = el.tags();
        Instant now = Instant.now();

        Station s = new Station();
        s.setName(resolveName(tags));
        s.setOperator(firstNonBlank(tags.get("operator"), tags.get("brand"), tags.get("network")));
        s.setAddress(resolveAddress(tags));
        s.setArea(firstNonBlank(tags.get("addr:city"), tags.get("addr:suburb"), tags.get("addr:province")));
        s.setLocation(new GeoJsonPoint(el.lon(), el.lat())); // GeoJSON [lng, lat]
        s.setOpeningHours(tags.get("opening_hours"));
        s.setPhone(firstNonBlank(tags.get("phone"), tags.get("contact:phone")));
        s.setAmenities(new ArrayList<>());
        s.setRating(null);

        List<Charger> chargers = buildChargers(tags);
        s.setChargers(chargers);
        s.setTotalChargers(chargers.size());
        // Availability is genuinely unknown for imported data — never fabricate it.
        s.setAvailableCount(0);
        s.setAvailabilitySummary(AvailabilitySummary.UNKNOWN);
        s.setAvailabilityUpdatedAt(null);

        // Price is unknown from OSM (fee=yes/no doesn't give a rate). Leave null.
        s.setCurrentPricing(null);

        s.setDataSource(DataSource.OPEN_DATASET);
        s.setConfidence(Confidence.LOW);
        s.setLastVerified(null);
        s.setSourceRef(el.sourceRef());
        s.setCreatedAt(now);
        s.setLastUpdated(now);
        return s;
    }

    /**
     * Copies importable fields from a freshly-mapped station onto an existing one,
     * WITHOUT clobbering community-owned data (availability, price, rating, likes,
     * image, branch, disabled flag). Used on re-import to refresh OSM facts only.
     */
    public void applyImportableFields(Station existing, Station fresh) {
        existing.setName(fresh.getName());
        existing.setOperator(fresh.getOperator());
        existing.setAddress(fresh.getAddress());
        existing.setArea(fresh.getArea());
        existing.setLocation(fresh.getLocation());
        existing.setOpeningHours(fresh.getOpeningHours());
        existing.setPhone(fresh.getPhone());
        existing.setChargers(fresh.getChargers());
        existing.setTotalChargers(fresh.getTotalChargers());
        existing.setLastUpdated(Instant.now());
        // dataSource/confidence/sourceRef unchanged; availability, price, rating,
        // likeCount, imageId, branchId, disabled all preserved.
    }

    private String resolveName(Map<String, String> tags) {
        String name = firstNonBlank(tags.get("name"), tags.get("name:en"),
                tags.get("operator"), tags.get("brand"));
        return name != null ? name : "EV Charging Station";
    }

    private String resolveAddress(Map<String, String> tags) {
        List<String> parts = new ArrayList<>();
        addIfPresent(parts, tags.get("addr:housenumber"));
        addIfPresent(parts, tags.get("addr:street"));
        addIfPresent(parts, tags.get("addr:suburb"));
        addIfPresent(parts, tags.get("addr:city"));
        addIfPresent(parts, tags.get("addr:province"));
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private void addIfPresent(List<String> parts, String v) {
        if (v != null && !v.isBlank()) {
            parts.add(v.trim());
        }
    }

    /**
     * Derives chargers from {@code socket:*} tags. When none are present, falls back to a
     * single UNKNOWN-status charger inferred from generic power/type tags so the station
     * still shows a charger count.
     */
    private List<Charger> buildChargers(Map<String, String> tags) {
        List<Charger> chargers = new ArrayList<>();

        for (Map.Entry<String, ConnectorType> entry : SOCKET_CONNECTORS.entrySet()) {
            String socketKey = "socket:" + entry.getKey();
            String countRaw = tags.get(socketKey);
            if (countRaw == null) {
                continue;
            }
            int count = parseIntSafe(countRaw, 1);
            if (count <= 0) {
                count = 1;
            }
            count = Math.min(count, 20); // guard against absurd values
            double power = parsePower(
                    tags.get(socketKey + ":output"),
                    tags.get("maxpower"),
                    tags.get("charge"));
            ChargerType type = classify(power, entry.getValue());
            for (int i = 0; i < count; i++) {
                chargers.add(new Charger(new ObjectId(), entry.getValue(), type, power,
                        ChargerStatus.UNKNOWN, Instant.now()));
            }
        }

        if (chargers.isEmpty()) {
            // No socket tags — create one generic charger so the station isn't "0 chargers".
            double power = parsePower(tags.get("maxpower"), tags.get("charge"), null);
            ConnectorType connector = ConnectorType.TYPE2; // most common AC in PH
            ChargerType type = classify(power, connector);
            chargers.add(new Charger(new ObjectId(), connector, type, power,
                    ChargerStatus.UNKNOWN, Instant.now()));
        }
        return chargers;
    }

    /** Classifies AC/DC/DC_FAST from power and connector standard. */
    private ChargerType classify(double powerKw, ConnectorType connector) {
        boolean dcConnector = connector == ConnectorType.CCS1 || connector == ConnectorType.CCS2
                || connector == ConnectorType.CHADEMO || connector == ConnectorType.GBT
                || connector == ConnectorType.NACS;
        if (powerKw >= 50 || (dcConnector && powerKw >= 43)) {
            return ChargerType.DC_FAST;
        }
        if (dcConnector && powerKw >= 20) {
            return ChargerType.DC;
        }
        return ChargerType.AC;
    }

    /**
     * Parses a power value in kW from OSM tags. Handles "22 kW", "22000 W", "7.4", "50 kw".
     * Returns a conservative default of 7.0 kW when nothing parses.
     */
    private double parsePower(String... candidates) {
        for (String raw : candidates) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String v = raw.toLowerCase().trim();
            try {
                boolean watts = v.contains("w") && !v.contains("kw");
                String num = v.replaceAll("[^0-9.]", "");
                if (num.isBlank()) {
                    continue;
                }
                double parsed = Double.parseDouble(num);
                if (watts && parsed > 1000) {
                    parsed = parsed / 1000.0; // W -> kW
                }
                if (parsed > 0 && parsed < 1000) {
                    return Math.round(parsed * 10.0) / 10.0;
                }
            } catch (NumberFormatException ignored) {
                // try next candidate
            }
        }
        return 7.0;
    }

    private int parseIntSafe(String v, int fallback) {
        try {
            return Integer.parseInt(v.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return fallback;
        }
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }
}
