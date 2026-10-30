package com.mazlove.backend.tracking;

import static com.mazlove.backend.tracking.SessionStateMachine.*;

import com.mazlove.backend.models.Session;
import com.mazlove.backend.util.GeoUtils;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TrackingService {
  private final SessionAccessPolicy access;
  private final TrackingStore store;
  private final RoutingService routing;
  private final TrackingConfig config;
  private final SessionLifecycle lifecycle;
  private final SimpMessagingTemplate ws;
  private final JdbcTemplate db;
  private final TrackingPublisher publisher;

  public record PositionUpdate(
      double lat,
      double lng,
      Double heading,
      Double speed,
      long ts,
      long seq,
      Integer etaSeconds,
      Integer distanceMeters,
      String status,
      boolean estimated,
      Double accuracy) {}

  public record Snapshot(
      String status,
      MeetingPoint meetingPoint,
      PositionUpdate pos,
      RoutingService.Route route,
      String providerName,
      boolean signalLost) {}

  public Snapshot snapshot(String email, long id) {
    Session s = access.lockParticipant(email, id);
    return snapshot(s);
  }

  public Snapshot snapshot(Session s) {
    boolean live = s.getStatus() == Session.Status.EN_ROUTE;
    var p = live ? store.last(s.getId()) : null;
    var r = live ? store.route(s.getId()) : null;
    return new Snapshot(
        s.getStatus().name(),
        s.getStatus() == Session.Status.PENDING ? null : meeting(s),
        p == null ? null : update(p, s),
        r == null ? null : r.route(),
        s.getProvider().getUser().getName().split(" ")[0],
        live && store.get("lost", s.getId()) != null);
  }

  public static MeetingPoint meeting(Session s) {
    return s.getMeetingLatitude() == null
        ? null
        : new MeetingPoint(s.getMeetingLatitude(), s.getMeetingLongitude(), s.getMeetingLabel());
  }

  private PositionUpdate update(TrackingStore.Last l, Session s) {
    var p = l.point();
    return new PositionUpdate(
        p.lat(),
        p.lng(),
        p.heading(),
        p.speed(),
        l.receivedAt(),
        p.seq(),
        l.etaSeconds(),
        l.distanceMeters(),
        s.getStatus().name(),
        l.estimated(),
        p.accuracy());
  }

  public void ingest(String email, long id, PositionDTO p) {
    db.queryForList("SELECT id FROM sessions WHERE id=? FOR UPDATE", Long.class, id);
    Session s = access.lockProvider(email, id);
    if (s.getStatus() != Session.Status.EN_ROUTE) return;
    long now = System.currentTimeMillis();
    if (p == null
        || !Double.isFinite(p.lat())
        || !Double.isFinite(p.lng())
        || Math.abs(p.lat()) > 90
        || Math.abs(p.lng()) > 180
        || p.accuracy() == null
        || !Double.isFinite(p.accuracy())
        || p.accuracy() < 0
        || p.accuracy() > config.getMaxAccuracyM()
        || p.seq() < 0
        || p.ts() > now + 5000
        || p.ts() < now - 30000) return;
    if ((p.heading() != null
            && (!Double.isFinite(p.heading()) || p.heading() < 0 || p.heading() > 360))
        || (p.speed() != null && (!Double.isFinite(p.speed()) || p.speed() < 0))) return;
    var last = store.last(id);
    if (last != null && (p.seq() <= last.point().seq() || p.ts() <= last.point().ts())) return;
    if (!store.rate(id)) return;
    double[] point = {p.lng(), p.lat()}, goal = {s.getMeetingLongitude(), s.getMeetingLatitude()};
    if (last != null
        && GeoUtils.haversine(point, new double[] {last.point().lng(), last.point().lat()})
                / Math.max(.001, (p.ts() - last.point().ts()) / 1000.0)
            > config.getMaxSpeedMps()) return;
    if (store.get("lost", id) != null) {
      store.delete("lost", id);
      event(s, "SIGNAL_RECOVERED", "Signal retrouvé");
    }
    var cached = store.route(id);
    boolean deviated = false;
    if (cached != null
        && GeoUtils.distanceToPolyline(point, cached.route().coordinates())
            > config.getDeviationThresholdM()) {
      String off = store.get("off", id);
      if (off == null) store.set("off", id, Long.toString(now), 300);
      else if (now - Long.parseLong(off) > config.getDeviationDurationS() * 1000L
          && store.get("off-alert", id) == null) {
        deviated = true;
        store.set("off-alert", id, "1", 300);
        event(s, "DEVIATION", "Le prestataire a pris un autre chemin, l'ETA est recalculé");
      }
    } else {
      store.delete("off", id);
      store.delete("off-alert", id);
    }
    // Preserve the baseline while a deviation episode is pending, otherwise a periodic reroute
    // hides it.
    if (cached == null
        || deviated
        || (now - cached.at() > config.getEtaRefreshS() * 1000L && store.get("off", id) == null)) {
      store.route(id, routing.route(point, goal, s.getTravelMode()));
      cached = store.route(id);
    }
    RoutingService.Route route =
        cached == null
            ? new StraightLineRoutingService().route(point, goal, s.getTravelMode())
            : cached.route();
    double length = GeoUtils.length(route.coordinates());
    double fraction =
        length <= 0
            ? 0
            : Math.max(0, 1 - GeoUtils.progressAlongPolyline(point, route.coordinates()) / length);
    var saved =
        new TrackingStore.Last(
            p,
            now,
            (int) Math.ceil(route.durationSeconds() * fraction),
            (int) Math.ceil(route.distanceMeters() * fraction),
            route.estimated());
    store.save(id, saved);
    long near =
        GeoUtils.haversine(point, goal) < config.getArrivalRadiusM()
            ? store.increment("near", id, 60)
            : 0;
    if (near == 0) store.delete("near", id);
    if (near >= config.getArrivalConsecutivePings()) {
      lifecycle.move(s, Action.ARRIVE, Actor.SYSTEM);
      return;
    }
    PositionUpdate payload = update(saved, s);
    AfterCommit.run(() -> publisher.live(id, payload));
  }

  public void event(Session s, String type, String message) {
    db.update(
        "INSERT INTO tracking_events(session_id,event_type,details) VALUES (?,?,?)",
        s.getId(),
        type,
        message);
    long id = s.getId();
    String email = s.getClient().getEmail();
    AfterCommit.run(
        () -> {
          publisher.live(
              id, Map.of("event", type, "message", message, "at", System.currentTimeMillis()));
          ws.convertAndSendToUser(
              email, "/queue/notifications", Map.of("sessionId", id, "message", message));
        });
  }
}
