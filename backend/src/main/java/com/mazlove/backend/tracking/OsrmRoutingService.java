package com.mazlove.backend.tracking;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OsrmRoutingService implements RoutingService {
  private final RestTemplate http;
  private final String base;

  public OsrmRoutingService(
      @Value("${osrm.base-url:https://router.project-osrm.org}") String base,
      @Value("${osrm.timeout-ms:2000}") int timeout) {
    this.base = base;
    var f = new SimpleClientHttpRequestFactory();
    f.setConnectTimeout(timeout);
    f.setReadTimeout(timeout);
    http = new RestTemplate(f);
  }

  public Route route(double[] a, double[] b, TravelMode mode) {
    JsonNode r =
        http.getForObject(
                base
                    + String.format(
                        Locale.ROOT,
                        "/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=geojson",
                        a[0],
                        a[1],
                        b[0],
                        b[1]),
                JsonNode.class)
            .path("routes")
            .get(0);
    var points = new ArrayList<double[]>();
    for (JsonNode p : r.path("geometry").path("coordinates"))
      points.add(new double[] {p.get(0).asDouble(), p.get(1).asDouble()});
    if (points.size() < 2) throw new IllegalStateException("Itinéraire indisponible");
    double d = r.path("distance").asDouble();
    return new Route(
        points,
        d,
        mode == TravelMode.DRIVING ? r.path("duration").asDouble() : d / mode.speed,
        mode != TravelMode.DRIVING);
  }
}
