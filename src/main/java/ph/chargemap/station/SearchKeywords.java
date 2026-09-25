package ph.chargemap.station;

import ph.chargemap.charger.ChargerType;
import ph.chargemap.charger.ConnectorType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Maps free-text search terms to charger-type / connector enum values so that queries
 * like "DC fast" or "CCS" also match stations by their chargers (Requirement 5.2).
 */
public final class SearchKeywords {

    private SearchKeywords() {
    }

    public static List<ChargerType> matchChargerTypes(String query) {
        String q = query.toLowerCase(Locale.ROOT);
        List<ChargerType> types = new ArrayList<>();
        if (q.contains("dc fast") || q.contains("dcfast") || q.contains("fast")) {
            types.add(ChargerType.DC_FAST);
        }
        if (q.contains("dc") && !types.contains(ChargerType.DC_FAST)) {
            types.add(ChargerType.DC);
        }
        if (q.contains("ac")) {
            types.add(ChargerType.AC);
        }
        return types;
    }

    public static List<ConnectorType> matchConnectors(String query) {
        String q = query.toLowerCase(Locale.ROOT);
        List<ConnectorType> connectors = new ArrayList<>();
        if (q.contains("ccs2")) {
            connectors.add(ConnectorType.CCS2);
        }
        if (q.contains("ccs1")) {
            connectors.add(ConnectorType.CCS1);
        }
        if (q.contains("ccs") && !connectors.contains(ConnectorType.CCS2)
                && !connectors.contains(ConnectorType.CCS1)) {
            connectors.add(ConnectorType.CCS2);
            connectors.add(ConnectorType.CCS1);
        }
        if (q.contains("chademo")) {
            connectors.add(ConnectorType.CHADEMO);
        }
        if (q.contains("type 2") || q.contains("type2")) {
            connectors.add(ConnectorType.TYPE2);
        }
        if (q.contains("gb/t") || q.contains("gbt")) {
            connectors.add(ConnectorType.GBT);
        }
        if (q.contains("nacs") || q.contains("tesla")) {
            connectors.add(ConnectorType.NACS);
        }
        return connectors;
    }
}
