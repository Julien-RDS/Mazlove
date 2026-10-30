package com.mazlove.backend.tracking;

import static com.mazlove.backend.tracking.SessionStateMachine.*;

import com.mazlove.backend.models.Session;
import com.mazlove.backend.repository.SessionRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionLifecycle {
  public record Changed(Long id, String status, String message) {}

  private final SessionRepository sessions;
  private final JdbcTemplate db;
  private final ApplicationEventPublisher events;

  public boolean hasSos(long id) {
    return Boolean.TRUE.equals(
        db.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM sos_alerts WHERE session_id=?)", Boolean.class, id));
  }

  public void move(Session s, Action action, Actor actor) {
    Session.Status before = s.getStatus();
    s.setStatus(next(before, action, actor));
    LocalDateTime now = LocalDateTime.now();
    switch (s.getStatus()) {
      case EN_ROUTE -> s.setEnRouteAt(now);
      case ARRIVED -> s.setArrivedAt(now);
      case IN_PROGRESS -> s.setStartedAt(now);
      case DONE, CANCELLED -> s.setEndedAt(now);
      default -> {}
    }
    sessions.saveAndFlush(s);
    if (java.util.Set.of(Session.Status.ARRIVED, Session.Status.DONE, Session.Status.CANCELLED)
        .contains(s.getStatus()))
      db.update("UPDATE trip_shares SET revoked=true WHERE session_id=?", s.getId());
    String type =
        switch (action) {
          case DEPART -> "EN_ROUTE_STARTED";
          case ARRIVE -> actor == Actor.SYSTEM ? "ARRIVED_AUTO" : "ARRIVED_MANUAL";
          case CANCEL -> before == Session.Status.EN_ROUTE ? "CANCELLED_EN_ROUTE" : "CANCELLED";
          default -> action.name();
        };
    db.update("INSERT INTO tracking_events(session_id,event_type) VALUES (?,?)", s.getId(), type);
    events.publishEvent(
        new Changed(
            s.getId(),
            s.getStatus().name(),
            switch (s.getStatus()) {
              case EN_ROUTE -> "Votre prestataire est en route";
              case ARRIVED -> "Votre prestataire est arrivé";
              case IN_PROGRESS -> "La séance commence";
              case DONE -> "Séance terminée";
              case CANCELLED -> "Séance annulée";
              default -> "Réservation acceptée";
            }));
  }
}
