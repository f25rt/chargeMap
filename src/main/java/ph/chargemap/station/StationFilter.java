package ph.chargemap.station;

import org.springframework.data.mongodb.core.query.Criteria;
import ph.chargemap.availability.AvailabilitySummary;
import ph.chargemap.charger.ChargerType;
import ph.chargemap.charger.ConnectorType;
import ph.chargemap.common.error.BadRequestException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses and validates station filter parameters (Requirement 6) and builds the MongoDB
 * {@link Criteria} used by list and geo queries. Invalid enum values or negative kW are
 * rejected with {@link BadRequestException} (Requirement 6.3).
 */
public record StationFilter(
        ChargerType chargerType,
        ConnectorType connector,
        Double minKw,
        Double maxKw,
        java.math.BigDecimal priceMax,
        boolean availableOnly
) {

    public static StationFilter parse(String chargerType, String connector, Double minKw,
                                      Double maxKw, java.math.BigDecimal priceMax,
                                      Boolean availableOnly) {
        ChargerType parsedType = parseEnum(ChargerType.class, chargerType, "chargerType");
        ConnectorType parsedConnector = parseEnum(ConnectorType.class, connector, "connector");
        if (minKw != null && minKw < 0) {
            throw new BadRequestException("minKw must be >= 0");
        }
        if (maxKw != null && maxKw < 0) {
            throw new BadRequestException("maxKw must be >= 0");
        }
        if (minKw != null && maxKw != null && minKw > maxKw) {
            throw new BadRequestException("minKw must be <= maxKw");
        }
        if (priceMax != null && priceMax.signum() < 0) {
            throw new BadRequestException("priceMax must be >= 0");
        }
        return new StationFilter(parsedType, parsedConnector, minKw, maxKw, priceMax,
                Boolean.TRUE.equals(availableOnly));
    }

    /** True when no filter fields are set. */
    public boolean isEmpty() {
        return chargerType == null && connector == null && minKw == null && maxKw == null
                && priceMax == null && !availableOnly;
    }

    /**
     * Builds a Mongo criteria for the filters, or null when empty.
     *
     * @param stalenessCutoff instant before which availability is considered stale;
     *                        used only when {@code availableOnly} is set (Requirement 6.2)
     */
    public Criteria toCriteria(Instant stalenessCutoff) {
        List<Criteria> parts = new ArrayList<>();
        if (chargerType != null) {
            parts.add(Criteria.where("chargers.chargerType").is(chargerType.name()));
        }
        if (connector != null) {
            parts.add(Criteria.where("chargers.connectorType").is(connector.name()));
        }
        if (minKw != null || maxKw != null) {
            Criteria power = Criteria.where("chargers.powerKw");
            if (minKw != null) {
                power = power.gte(minKw);
            }
            if (maxKw != null) {
                power = power.lte(maxKw);
            }
            parts.add(power);
        }
        if (priceMax != null) {
            parts.add(Criteria.where("currentPricing.pricePerKwh").lte(priceMax));
        }
        if (availableOnly) {
            parts.add(Criteria.where("availableCount").gt(0));
            parts.add(Criteria.where("availabilitySummary").is(AvailabilitySummary.AVAILABLE.name()));
            parts.add(Criteria.where("availabilityUpdatedAt").gte(stalenessCutoff));
        }
        if (parts.isEmpty()) {
            return null;
        }
        return new Criteria().andOperator(parts.toArray(new Criteria[0]));
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid " + field + ": " + value);
        }
    }
}
