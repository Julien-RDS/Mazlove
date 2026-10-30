package com.mazlove.backend.tracking;

import com.fasterxml.jackson.databind.JsonNode;
import com.mazlove.backend.exception.*;
import io.swagger.v3.oas.annotations.Operation;
import java.security.Principal;
import java.time.Duration;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequiredArgsConstructor
public class DiscoveryTrackingController {
  private final StringRedisTemplate redis;
  private final JdbcTemplate db;
  private final SessionAccessPolicy access;

  public record Online(boolean online, Double lat, Double lng) {}

  @PutMapping("/api/providers/me/online")
  @Operation(summary = "Signaler sa disponibilité avec une position approximative")
  @org.springframework.transaction.annotation.Transactional
  public void online(Principal p, @RequestBody Online o) {
    var ids =
        db.queryForList(
            "SELECT id FROM providers WHERE user_id=? FOR UPDATE",
            Long.class,
            access.user(p.getName()).getId());
    if (ids.isEmpty()) throw new ForbiddenException("Fiche prestataire requise.");
    String id = ids.get(0).toString();
    if (!o.online()) {
      redis.opsForGeo().remove("geo:online", id);
      redis.delete("online:" + id);
      return;
    }
    if (o.lat() == null
        || o.lng() == null
        || !Double.isFinite(o.lat())
        || !Double.isFinite(o.lng())
        || Math.abs(o.lat()) > 85
        || Math.abs(o.lng()) > 180) throw new BadRequestException("Position invalide.");
    // Grid + stable jitter; exact coordinates never enter the public GEO set.
    double jitter = (Math.floorMod(ids.get(0) * 2654435761L, 1000) / 1000.0 - .5) * .0005;
    redis
        .opsForGeo()
        .add(
            "geo:online",
            new Point(
                Math.rint(o.lng() / .0028) * .0028 + jitter,
                Math.rint(o.lat() / .0018) * .0018 + jitter),
            id);
    redis.opsForValue().set("online:" + id, "1", Duration.ofSeconds(90));
    redis.expire("geo:online", Duration.ofMinutes(2));
  }

  @GetMapping("/api/providers/online")
  @Operation(summary = "Prestataires disponibles, positions floutées")
  public Object nearby(
      @RequestParam double lat,
      @RequestParam double lng,
      @RequestParam(defaultValue = "5000") double radius) {
    if (!Double.isFinite(lat)
        || !Double.isFinite(lng)
        || !Double.isFinite(radius)
        || Math.abs(lat) > 85
        || Math.abs(lng) > 180
        || radius <= 0) throw new BadRequestException("Recherche invalide.");
    var result =
        redis
            .opsForGeo()
            .search(
                "geo:online",
                GeoReference.fromCoordinate(lng, lat),
                new Distance(Math.min(radius, 50000) / 1000, Metrics.KILOMETERS),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                    .includeCoordinates()
                    .includeDistance()
                    .sortAscending()
                    .limit(100));
    List<Object> out = new ArrayList<>();
    if (result != null)
      for (var r : result) {
        var c = r.getContent();
        if (Boolean.TRUE.equals(redis.hasKey("online:" + c.getName())))
          out.add(
              Map.of(
                  "providerId",
                  c.getName(),
                  "lat",
                  c.getPoint().getY(),
                  "lng",
                  c.getPoint().getX(),
                  "distanceM",
                  r.getDistance().getValue() * 1000));
      }
    return out;
  }

  @Scheduled(fixedDelay = 30000)
  public void purge() {
    var members = redis.opsForZSet().range("geo:online", 0, -1);
    if (members != null)
      for (String id : members)
        if (!Boolean.TRUE.equals(redis.hasKey("online:" + id)))
          redis.opsForGeo().remove("geo:online", id);
  }

  @GetMapping("/api/geocode")
  @Operation(summary = "Rechercher une adresse française")
  public Object geocode(@RequestParam String q) {
    if (q.length() < 3 || q.length() > 200)
      throw new BadRequestException("Saisissez entre 3 et 200 caractères.");
    var f = new SimpleClientHttpRequestFactory();
    f.setConnectTimeout(2000);
    f.setReadTimeout(2000);
    try {
      return new RestTemplate(f)
          .getForObject(
              UriComponentsBuilder.fromHttpUrl("https://data.geopf.fr/geocodage/search")
                  .queryParam("q", q)
                  .queryParam("limit", 5)
                  .build()
                  .encode()
                  .toUri(),
              JsonNode.class);
    } catch (Exception e) {
      return Map.of("features", List.of());
    }
  }
}
