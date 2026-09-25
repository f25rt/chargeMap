package ph.chargemap.charger;

import java.time.Instant;

/** Charger representation returned to clients. */
public record ChargerDto(
        String chargerId,
        ConnectorType connectorType,
        ChargerType chargerType,
        double powerKw,
        ChargerStatus status,
        Instant statusUpdatedAt
) {
    public static ChargerDto from(Charger c) {
        return new ChargerDto(
                c.getChargerId() == null ? null : c.getChargerId().toHexString(),
                c.getConnectorType(),
                c.getChargerType(),
                c.getPowerKw(),
                c.getStatus(),
                c.getStatusUpdatedAt()
        );
    }
}
