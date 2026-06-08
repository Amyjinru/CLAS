package com.clas.common;

import java.math.BigDecimal;

public final class GeoUtils {
    private static final double EARTH_RADIUS_METERS = 6371000.0;

    private GeoUtils() {
    }

    public static int distanceMeters(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        return (int) Math.round(distanceMeters(
            lat1.doubleValue(),
            lng1.doubleValue(),
            lat2.doubleValue(),
            lng2.doubleValue()
        ));
    }

    public static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
            + Math.cos(radLat1) * Math.cos(radLat2)
            * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);

        return 2 * EARTH_RADIUS_METERS * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public static boolean hasCoordinate(BigDecimal longitude, BigDecimal latitude) {
        return longitude != null && latitude != null;
    }
}
