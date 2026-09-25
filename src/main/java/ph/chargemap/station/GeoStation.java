package ph.chargemap.station;

/**
 * A {@link Station} paired with the {@code distanceMeters} computed by a {@code $geoNear}
 * stage. This is a plain holder (not a {@code @Document}) so it does not inherit the
 * station's index annotations, which would otherwise conflict on the shared collection.
 */
public class GeoStation {

    private final Station station;
    private final Double distanceMeters;

    public GeoStation(Station station, Double distanceMeters) {
        this.station = station;
        this.distanceMeters = distanceMeters;
    }

    public Station getStation() {
        return station;
    }

    public Double getDistanceMeters() {
        return distanceMeters;
    }

    public String getName() {
        return station.getName();
    }
}
