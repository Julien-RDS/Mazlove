package com.mazlove.backend.tracking;

import java.util.List;

public interface RoutingService {
  record Route(
      List<double[]> coordinates,
      double distanceMeters,
      double durationSeconds,
      boolean estimated) {}

  Route route(double[] from, double[] to, TravelMode mode);
}
