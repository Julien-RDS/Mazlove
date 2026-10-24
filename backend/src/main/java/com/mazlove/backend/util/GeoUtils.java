package com.mazlove.backend.util;

/**
 * Calculs géographiques simples (sphère terrestre de rayon moyen 6371 km).
 * Précision de l'ordre de 0,5 % : largement suffisant pour « à 2,3 km ».
 */
public final class GeoUtils {

    public static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
    }

    /** Distance orthodromique (formule de Haversine) entre deux points, en kilomètres. */
    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_KM * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Rectangle qui contient le cercle de rayon {@code radiusKm} autour du point.
     * Sert de pré-filtre indexable avant le calcul exact de distance en SQL.
     * Limite connue : ne gère pas l'antiméridien (longitude ±180°), sans impact pour la France.
     */
    public static BoundingBox boundingBox(double lat, double lng, double radiusKm) {
        double dLat = Math.toDegrees(radiusKm / EARTH_RADIUS_KM);
        double cosLat = Math.cos(Math.toRadians(lat));
        // Près des pôles, un degré de longitude tend vers 0 km : on ouvre toute la plage.
        double dLng = cosLat < 1e-6 ? 180.0 : Math.toDegrees(radiusKm / (EARTH_RADIUS_KM * cosLat));
        return new BoundingBox(
                Math.max(-90.0, lat - dLat),
                Math.min(90.0, lat + dLat),
                Math.max(-180.0, lng - dLng),
                Math.min(180.0, lng + dLng));
    }

    /** Arrondi décimal classique (2 décimales ≈ 1 km en latitude, 3 décimales ≈ 110 m). */
    public static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    public record BoundingBox(double minLat, double maxLat, double minLng, double maxLng) {
    }
}
