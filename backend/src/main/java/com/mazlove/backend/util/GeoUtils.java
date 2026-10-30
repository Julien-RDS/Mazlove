package com.mazlove.backend.util;

/**
 * Calculs géographiques simples (sphère terrestre de rayon moyen 6371 km). Précision de l'ordre de
 * 0,5 % : largement suffisant pour « à 2,3 km ».
 */
public final class GeoUtils {

  public static final double EARTH_RADIUS_KM = 6371.0;

  private GeoUtils() {}

  /** Distance orthodromique (formule de Haversine) entre deux points, en kilomètres. */
  public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
    double dLat = Math.toRadians(lat2 - lat1);
    double dLng = Math.toRadians(lng2 - lng1);
    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2)
                * Math.sin(dLng / 2);
    return 2 * EARTH_RADIUS_KM * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  /**
   * Rectangle qui contient le cercle de rayon {@code radiusKm} autour du point. Sert de pré-filtre
   * indexable avant le calcul exact de distance en SQL. Limite connue : ne gère pas l'antiméridien
   * (longitude ±180°), sans impact pour la France.
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

  public static double haversine(double[] a, double[] b) {
    return distanceKm(a[1], a[0], b[1], b[0]) * 1000;
  }

  public static double bearing(double[] a, double[] b) {
    double p = Math.toRadians(a[1]), q = Math.toRadians(b[1]), d = Math.toRadians(b[0] - a[0]);
    return (Math.toDegrees(
                Math.atan2(
                    Math.sin(d) * Math.cos(q),
                    Math.cos(p) * Math.sin(q) - Math.sin(p) * Math.cos(q) * Math.cos(d)))
            + 360)
        % 360;
  }

  public record Projection(double distance, double progress) {}

  public static Projection project(double[] p, java.util.List<double[]> line) {
    double best = Double.POSITIVE_INFINITY,
        progress = 0,
        total = 0,
        c = Math.cos(Math.toRadians(p[1]));
    for (int i = 1; i < line.size(); i++) {
      double[] a = line.get(i - 1), b = line.get(i);
      double ax = (a[0] - p[0]) * c * 111195,
          ay = (a[1] - p[1]) * 111195,
          bx = (b[0] - p[0]) * c * 111195,
          by = (b[1] - p[1]) * 111195;
      double dx = bx - ax,
          dy = by - ay,
          n = dx * dx + dy * dy,
          t = n == 0 ? 0 : Math.max(0, Math.min(1, -(ax * dx + ay * dy) / n));
      double d = Math.hypot(ax + t * dx, ay + t * dy), len = haversine(a, b);
      if (d < best) {
        best = d;
        progress = total + t * len;
      }
      total += len;
    }
    return new Projection(best, progress);
  }

  public static double distanceToPolyline(double[] p, java.util.List<double[]> line) {
    return project(p, line).distance();
  }

  public static double progressAlongPolyline(double[] p, java.util.List<double[]> line) {
    return project(p, line).progress();
  }

  public static double length(java.util.List<double[]> line) {
    double d = 0;
    for (int i = 1; i < line.size(); i++) d += haversine(line.get(i - 1), line.get(i));
    return d;
  }

  public record BoundingBox(double minLat, double maxLat, double minLng, double maxLng) {}
}
