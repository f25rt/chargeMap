package ph.chargemap.station.imports;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Thin client for the OpenStreetMap Overpass API. Fetches {@code amenity=charging_station}
 * elements within a bounding box.
 *
 * <p>OSM data is licensed under the Open Database License (ODbL); imported stations must
 * carry attribution to OpenStreetMap contributors. See DEPLOY / UI attribution.
 */
@Component
public class OverpassClient {

    private static final Logger log = LoggerFactory.getLogger(OverpassClient.class);

    private final RestClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public OverpassClient(
            @org.springframework.beans.factory.annotation.Value(
                "${chargemap.import.osm.endpoint:https://overpass-api.de/api/interpreter}")
            String endpoint) {
        // Overpass REQUIRES a descriptive User-Agent; requests without one get 406.
        // overpass-api.de uses a CA the JDK trusts out of the box. Keep import bounding
        // boxes regional (not country-wide) or this endpoint may 504. Override via
        // chargemap.import.osm.endpoint (e.g. a mirror), but check the JDK trusts its CA.
        this.http = RestClient.builder()
                .baseUrl(endpoint)
                .defaultHeader("User-Agent", "ChargeMapPH/1.0 (+https://chargemap-web.onrender.com)")
                .requestFactory(clientHttpRequestFactory())
                .build();
    }

    private static org.springframework.http.client.ClientHttpRequestFactory clientHttpRequestFactory() {
        var settings = org.springframework.boot.web.client.ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(15))
                .withReadTimeout(Duration.ofSeconds(120)); // Overpass can be slow
        return org.springframework.boot.web.client.ClientHttpRequestFactories.get(settings);
    }

    /**
     * Returns raw charging-station elements within the given bounding box.
     * Coordinates are decimal degrees: south &lt; north, west &lt; east.
     */
    public List<OverpassElement> fetchChargingStations(double south, double west,
                                                        double north, double east) {
        // Overpass QL: nodes, ways, and relations tagged amenity=charging_station.
        // `out center` gives ways/relations a representative lat/lon.
        String bbox = south + "," + west + "," + north + "," + east;
        String query = """
                [out:json][timeout:90];
                (
                  node["amenity"="charging_station"](%s);
                  way["amenity"="charging_station"](%s);
                  relation["amenity"="charging_station"](%s);
                );
                out center tags;
                """.formatted(bbox, bbox, bbox);

        log.info("Querying Overpass for charging stations in bbox [{}]", bbox);

        String body = http.post()
                // baseUrl is already the full interpreter endpoint; post to it directly.
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body("data=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8))
                .retrieve()
                .body(String.class);

        return parse(body);
    }

    /**
     * Parses a raw Overpass JSON body (as saved to a file or returned by the API) into
     * elements. Exposed so imports can run from a pre-fetched file when the backend can't
     * reach Overpass directly (e.g. behind a TLS-inspecting corporate proxy).
     */
    public List<OverpassElement> parseBody(String body) {
        return parse(body);
    }

    private List<OverpassElement> parse(String body) {
        List<OverpassElement> out = new ArrayList<>();
        if (body == null || body.isBlank()) {
            return out;
        }
        // Strip a UTF-8 BOM if present (files saved by some editors/tools include one,
        // which Jackson rejects as an unexpected leading character).
        if (!body.isEmpty() && body.charAt(0) == '\uFEFF') {
            body = body.substring(1);
        }
        try {
            JsonNode root = mapper.readTree(body);
            JsonNode elements = root.path("elements");
            if (!elements.isArray()) {
                return out;
            }
            for (JsonNode el : elements) {
                String type = el.path("type").asText(null);
                long id = el.path("id").asLong();
                if (type == null || id == 0) {
                    continue;
                }
                // node has lat/lon directly; way/relation expose "center".
                double lat;
                double lon;
                if (el.has("lat") && el.has("lon")) {
                    lat = el.path("lat").asDouble();
                    lon = el.path("lon").asDouble();
                } else if (el.has("center")) {
                    lat = el.path("center").path("lat").asDouble();
                    lon = el.path("center").path("lon").asDouble();
                } else {
                    continue; // no usable coordinate
                }

                var tags = new java.util.LinkedHashMap<String, String>();
                JsonNode tagNode = el.path("tags");
                if (tagNode.isObject()) {
                    tagNode.fields().forEachRemaining(e -> tags.put(e.getKey(), e.getValue().asText()));
                }
                out.add(new OverpassElement(type, id, lat, lon, tags));
            }
        } catch (Exception e) {
            log.error("Failed to parse Overpass response: {}", e.getMessage());
        }
        log.info("Overpass returned {} charging-station elements", out.size());
        return out;
    }

    /** A single OSM element (node/way/relation) with a resolved coordinate + tags. */
    public record OverpassElement(String type, long id, double lat, double lon,
                                  java.util.Map<String, String> tags) {

        /** Stable source reference, e.g. {@code osm:node/123456}. */
        public String sourceRef() {
            return "osm:" + type + "/" + id;
        }
    }
}
