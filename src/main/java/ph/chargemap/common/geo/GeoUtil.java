package ph.chargemap.common.geo;

import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import ph.chargemap.common.error.BadRequestException;

/**
 * Geospatial helpers: conversion between the client {@link GeoPoint} ({lat, lng}) and
 * MongoDB {@link GeoJsonPoint} ([lng, lat]), distance unit conversion, and coordinate
 * validation. Centralizing the coordinate-order swap avoids lng/lat mistakes.
 */
public final class GeoUtil {

    private static final double METERS_PER_KM = 1000.0;

    private GeoUtil() {
    }

    /** Converts a client point to a MongoDB GeoJSON point (note the coordinate swap). */
    public static GeoJsonPoint toGeoJson(GeoPoint point) {
        return new GeoJsonPoint(point.lng(), point.lat());
    }

    /** Converts a MongoDB GeoJSON point to a client point (note the coordinate swap). */
    public static GeoPoint toGeoPoint(GeoJsonPoint point) {
        // GeoJsonPoint stores x = longitude, y = latitude.
        return new GeoPoint(point.getY(), point.getX());
    }

    public static double kmToMeters(double km) {
        return km * METERS_PER_KM;
    }

    public static double metersToKm(double meters) {
        return meters / METERS_PER_KM;
    }

    /**
     * Validates latitude/longitude ranges (Requirement 2.2).
     *
     * @throws BadRequestException if out of range
     */
    public static void validateLatLng(double lat, double lng) {
        if (lat < -90 || lat > 90) {
            throw new BadRequestException("lat must be between -90 and 90");
        }
        if (lng < -180 || lng > 180) {
            throw new BadRequestException("lng must be between -180 and 180");
        }
    }

    /**
     * Validates and resolves a radius in km against a default and maximum.
     *
     * @param radiusKm    requested radius, may be null
     * @param defaultKm   default when null
     * @param maxKm       upper bound (inclusive)
     * @return resolved radius in km
     * @throws BadRequestException if non-positive or above the maximum
     */
    public static double resolveRadiusKm(Double radiusKm, double defaultKm, double maxKm) {
        double resolved = (radiusKm == null) ? defaultKm : radiusKm;
        if (resolved <= 0) {
            throw new BadRequestException("radius must be greater than 0");
        }
        if (resolved > maxKm) {
            throw new BadRequestException("radius must be <= " + maxKm + " km");
        }
        return resolved;
    }
}
