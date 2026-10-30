package com.mazlove.backend.tracking;

import com.mazlove.backend.util.GeoUtils;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StraightLineRoutingService implements RoutingService {
  public Route route(double[] a, double[] b, TravelMode m) {
    double d = GeoUtils.haversine(a, b) * 1.3;
    return new Route(List.of(a, b), d, d / m.speed, true);
  }
}
