package com.mazlove.backend.tracking;

import static org.junit.jupiter.api.Assertions.*;

import com.mazlove.backend.util.GeoUtils;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrackingGeoTest {
  @Test
  void knownDistanceAndBearing() {
    assertEquals(111195, GeoUtils.haversine(new double[] {0, 0}, new double[] {1, 0}), 2);
    assertEquals(90, GeoUtils.bearing(new double[] {0, 0}, new double[] {1, 0}), .001);
  }

  @Test
  void projectionAndProgress() {
    var line = List.of(new double[] {0, 0}, new double[] {.02, 0});
    assertEquals(111.195, GeoUtils.distanceToPolyline(new double[] {.01, .001}, line), .1);
    assertEquals(1111.95, GeoUtils.progressAlongPolyline(new double[] {.01, .001}, line), .1);
  }

  @Test
  void repeatedVertices() {
    assertTrue(
        Double.isFinite(
            GeoUtils.distanceToPolyline(
                new double[] {0, 0}, List.of(new double[] {0, 0}, new double[] {0, 0}))));
  }
}
