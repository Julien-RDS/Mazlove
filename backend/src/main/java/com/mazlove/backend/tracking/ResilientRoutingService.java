package com.mazlove.backend.tracking;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Primary
@RequiredArgsConstructor
public class ResilientRoutingService implements RoutingService {
  private final OsrmRoutingService osrm;
  private final StraightLineRoutingService fallback;
  private final StringRedisTemplate redis;
  private final ObjectMapper json;

  public Route route(double[] a, double[] b, TravelMode m) {
    String key =
        String.format(Locale.ROOT, "route:%s:%.4f,%.4f:%.4f,%.4f", m, a[1], a[0], b[1], b[0]);
    try {
      String cached = redis.opsForValue().get(key);
      if (cached != null) return json.readValue(cached, Route.class);
    } catch (Exception ignored) {
    }
    Route r;
    try {
      r = osrm.route(a, b, m);
    } catch (Exception e) {
      r = fallback.route(a, b, m);
    }
    try {
      redis.opsForValue().set(key, json.writeValueAsString(r), Duration.ofMinutes(10));
    } catch (Exception ignored) {
    }
    return r;
  }
}
