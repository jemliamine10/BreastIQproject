package com.breastcancer.breastcancerbackend.service;

public final class GeoUtils {

    private GeoUtils() {}

    public static boolean isValidLatLon(Double lat, Double lon) {
        if (lat == null || lon == null) return false;
        return lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }

    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0088;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
