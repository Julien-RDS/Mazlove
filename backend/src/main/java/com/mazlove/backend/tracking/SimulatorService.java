package com.mazlove.backend.tracking;

import static com.mazlove.backend.tracking.SessionStateMachine.*;

import com.mazlove.backend.models.Session;
import com.mazlove.backend.repository.SessionRepository;
import com.mazlove.backend.util.GeoUtils;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Profile("dev & !prod")
@RequiredArgsConstructor
public class SimulatorService {
  public enum Scenario {
    HAPPY_PATH,
    SLOW_TRAFFIC,
    SIGNAL_LOSS,
    DEVIATION,
    TELEPORT,
    CANCEL_EN_ROUTE
  }

  private final TrackingService tracking;
  private final RoutingService routing;
  private final SessionAccessPolicy access;
  private final SessionLifecycle lifecycle;
  private final TransactionTemplate tx;
  private final SessionRepository sessions;
  private final TrackingStore store;

  private static class Run {
    String email;
    Scenario scenario;
    List<double[]> line;
    double progress = 0, speed;
    int ticks = 0;
    long seq = System.currentTimeMillis();
    boolean paused = false;

    Run(String e, Scenario s, List<double[]> l, double v) {
      email = e;
      scenario = s;
      line = l;
      speed = v;
    }
  }

  private final Map<Long, Run> runs = new ConcurrentHashMap<>();

  public void start(String email, long id, Scenario scenario, double speed) {
    tx.executeWithoutResult(
        t -> {
          Session s = access.assertProvider(email, id);
          if (s.getStatus() != Session.Status.EN_ROUTE)
            throw new IllegalStateException("Appuyez sur Je pars avant la simulation.");
          double[] to = {s.getMeetingLongitude(), s.getMeetingLatitude()},
              from = {to[0] - .025, to[1] + .015};
          var route = routing.route(from, to, s.getTravelMode());
          // Keep accelerated demo points below the same 70 m/s anti-teleportation ceiling.
          runs.put(
              id,
              new Run(
                  email,
                  scenario,
                  route.coordinates(),
                  Math.min(60, s.getTravelMode().speed * Math.max(1, Math.min(20, speed)))));
        });
  }

  public void stop(long id) {
    runs.remove(id);
  }

  @Scheduled(fixedDelay = 1100)
  public void tick() {
    for (var entry : runs.entrySet())
      try {
        step(entry.getKey(), entry.getValue());
      } catch (RuntimeException e) {
        runs.remove(entry.getKey());
      }
  }

  private void step(long id, Run r) {
    tx.executeWithoutResult(
        t -> {
          Session s = access.assertProvider(r.email, id);
          if (s.getStatus() != Session.Status.EN_ROUTE) {
            stop(id);
            return;
          }
          r.ticks++;
          if (r.scenario == Scenario.CANCEL_EN_ROUTE && r.ticks == 10) {
            lifecycle.move(s, Action.CANCEL, Actor.PROVIDER);
            stop(id);
            return;
          }
          if (r.scenario == Scenario.SIGNAL_LOSS && r.ticks >= 15 && r.ticks < 71) return;
          if (r.scenario == Scenario.TELEPORT && r.ticks == 8) {
            tracking.ingest(
                r.email,
                id,
                new PositionDTO(40, 1, 5., 0., 5., System.currentTimeMillis(), ++r.seq));
            return;
          }
          r.progress += r.speed * 1.1 / (r.scenario == Scenario.SLOW_TRAFFIC ? 3 : 1);
          double[] p = pointAt(r.line, r.progress);
          // Gradual 600m detour, held >60s so it exercises sustained deviation, not teleportation.
          if (r.scenario == Scenario.DEVIATION && r.ticks >= 10 && r.ticks < 115) {
            r.progress = Math.min(r.progress, 600);
            p = pointAt(r.line, r.progress);
            double offset = Math.min(600, (r.ticks - 10) * 35);
            if (r.ticks > 95) offset = Math.max(0, 600 - (r.ticks - 95) * 35);
            p[1] += offset / 111195;
          }
          double noise = (Math.random() - .5) * .000025;
          tracking.ingest(
              r.email,
              id,
              new PositionDTO(
                  p[1] + noise,
                  p[0] + noise,
                  5.,
                  null,
                  r.speed,
                  System.currentTimeMillis(),
                  ++r.seq));
        });
  }

  public static double[] pointAt(List<double[]> line, double distance) {
    for (int i = 1; i < line.size(); i++) {
      double len = GeoUtils.haversine(line.get(i - 1), line.get(i));
      if (distance <= len) {
        double f = len == 0 ? 0 : distance / len;
        var a = line.get(i - 1);
        var b = line.get(i);
        return new double[] {a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f};
      }
      distance -= len;
    }
    return line.get(line.size() - 1).clone();
  }
}
