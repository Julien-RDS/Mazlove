package com.mazlove.backend.tracking;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrackingStore {
  private final StringRedisTemplate redis;
  private final ObjectMapper json;

  public record Last(
      PositionDTO point,
      long receivedAt,
      Integer etaSeconds,
      Integer distanceMeters,
      boolean estimated) {}

  public record CachedRoute(RoutingService.Route route, long at) {}

  public Last last(long id) {
    try {
      Object s = redis.opsForHash().get("track:last:" + id, "data");
      return s == null ? null : json.readValue(s.toString(), Last.class);
    } catch (Exception e) {
      return null;
    }
  }

  public void save(long id, Last p) {
    try {
      var fields = new java.util.HashMap<String, String>();
      fields.put("data", json.writeValueAsString(p));
      fields.put("lat", Double.toString(p.point().lat()));
      fields.put("lng", Double.toString(p.point().lng()));
      fields.put("acc", String.valueOf(p.point().accuracy()));
      fields.put("heading", String.valueOf(p.point().heading()));
      fields.put("speed", String.valueOf(p.point().speed()));
      fields.put("ts", Long.toString(p.receivedAt()));
      fields.put("seq", Long.toString(p.point().seq()));
      fields.put("eta", String.valueOf(p.etaSeconds()));
      fields.put("dist", String.valueOf(p.distanceMeters()));
      List<String> args = new ArrayList<>();
      fields.forEach(
          (k, v) -> {
            args.add(k);
            args.add(v);
          });
      redis.execute(
          new DefaultRedisScript<Long>(
              "redis.call('HSET',KEYS[1],unpack(ARGV)); redis.call('EXPIRE',KEYS[1],120); return 1",
              Long.class),
          List.of("track:last:" + id),
          args.toArray());
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  public CachedRoute route(long id) {
    try {
      String s = redis.opsForValue().get("track:route:" + id);
      return s == null ? null : json.readValue(s, CachedRoute.class);
    } catch (Exception e) {
      return null;
    }
  }

  public void route(long id, RoutingService.Route r) {
    try {
      redis
          .opsForValue()
          .set(
              "track:route:" + id,
              json.writeValueAsString(new CachedRoute(r, System.currentTimeMillis())),
              Duration.ofHours(14));
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  public boolean rate(long id) {
    return Boolean.TRUE.equals(
        redis.opsForValue().setIfAbsent("track:rate:" + id, "1", Duration.ofMillis(1000)));
  }

  public long increment(String key, long id, int seconds) {
    Long n =
        redis.execute(
            new DefaultRedisScript<Long>(
                "local n=redis.call('INCR',KEYS[1]); redis.call('EXPIRE',KEYS[1],ARGV[1]); return"
                    + " n",
                Long.class),
            List.of("track:" + key + ":" + id),
            String.valueOf(seconds));
    return n == null ? 0 : n;
  }

  public String get(String key, long id) {
    return redis.opsForValue().get("track:" + key + ":" + id);
  }

  public void set(String key, long id, String val, int ttl) {
    redis.opsForValue().set("track:" + key + ":" + id, val, Duration.ofSeconds(ttl));
  }

  public void delete(String key, long id) {
    redis.delete("track:" + key + ":" + id);
  }

  public void clear(long id) {
    for (String k : List.of("last", "rate", "route", "near", "off", "off-alert", "lost"))
      delete(k, id);
  }
}
