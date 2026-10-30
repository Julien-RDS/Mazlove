package com.mazlove.backend.tracking;

import com.mazlove.backend.exception.*;
import com.mazlove.backend.service.JWTService;
import java.security.Principal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("dev & !prod")
@RequiredArgsConstructor
public class DevSimulatorController {
  private final SimulatorService simulator;
  private final JWTService jwt;
  private final JdbcTemplate db;
  private final SessionAccessPolicy access;
  private final TrackingStore store;

  @PostMapping("/api/dev/login-as")
  public Object login(@RequestBody Map<String, String> body) {
    String email = body.get("email");
    if (!Set.of("alice@test.com", "sophie@test.com").contains(email))
      throw new ForbiddenException("Compte de démonstration uniquement.");
    access.user(email);
    return Map.of("token", jwt.generateToken(email));
  }

  @GetMapping("/api/dev/tracking/scenarios")
  public Object scenarios() {
    return SimulatorService.Scenario.values();
  }

  @GetMapping("/api/dev/demo-session")
  public Object demo(Principal p) {
    return Map.of("id", demoId());
  }

  private Long demoId() {
    var ids =
        db.queryForList("SELECT id FROM sessions WHERE notes='seed:tracking-demo'", Long.class);
    if (ids.isEmpty()) throw new NotFoundException("Chargez le seed de démonstration.");
    return ids.get(0);
  }

  @PostMapping("/api/dev/tracking/{id}/simulate")
  public void start(
      Principal p,
      @PathVariable long id,
      @RequestParam(defaultValue = "HAPPY_PATH") SimulatorService.Scenario scenario,
      @RequestParam(defaultValue = "10") double speedFactor) {
    simulator.start(p.getName(), id, scenario, speedFactor);
  }

  @PostMapping("/api/dev/tracking/{id}/stop")
  public void stop(Principal p, @PathVariable long id) {
    access.assertProvider(p.getName(), id);
    simulator.stop(id);
  }

  @PostMapping("/api/dev/reset-demo")
  @Transactional
  public Object reset(Principal p) {
    long id = demoId();
    access.lockProvider(p.getName(), id);
    simulator.stop(id);
    store.clear(id);
    db.update(
        "UPDATE sessions SET"
            + " version=version+1,status='ACCEPTED',datetime=CURRENT_TIMESTAMP+INTERVAL '1"
            + " day',travel_mode=NULL,en_route_at=NULL,arrived_at=NULL,started_at=NULL,ended_at=NULL"
            + " WHERE id=?",
        id);
    db.update("DELETE FROM tracking_events WHERE session_id=?", id);
    db.update("DELETE FROM trip_shares WHERE session_id=?", id);
    db.update("DELETE FROM sos_alerts WHERE session_id=?", id);
    return Map.of("id", id);
  }
}
