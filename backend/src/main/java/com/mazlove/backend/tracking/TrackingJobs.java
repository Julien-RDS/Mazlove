package com.mazlove.backend.tracking;

import com.mazlove.backend.repository.SessionRepository;
import java.time.*;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.event.*;

@Component
@RequiredArgsConstructor
public class TrackingJobs {
  private final com.mazlove.backend.service.EmailService emailService;
  private final JdbcTemplate db;
  private final TrackingStore store;
  private final SessionRepository sessions;
  private final TrackingService tracking;
  private final TrackingConfig config;
  private final SimpMessagingTemplate ws;

  @Value("${tracking.retention.events-days:30}")
  int retention;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void changed(SessionLifecycle.Changed e) {
    if (java.util.Set.of("ARRIVED", "DONE", "CANCELLED").contains(e.status())) {
      store.clear(e.id());
      db.update("UPDATE trip_shares SET revoked=true WHERE session_id=?", e.id());
    }
    ws.convertAndSend(
        "/topic/session." + e.id() + ".status",
        Map.of("status", e.status(), "message", e.message(), "at", System.currentTimeMillis()));
    sessions
        .findById(e.id())
        .ifPresent(
            s -> {
              if (e.status().equals("DONE"))
                for (String recipient :
                    java.util.List.of(
                        s.getClient().getEmail(), s.getProvider().getUser().getEmail()))
                  try {
                    emailService.sendText(
                        recipient,
                        "Votre séance Mazlove est terminée",
                        "La séance est terminée. Le partage de position est arrêté et les liens de"
                            + " suivi sont révoqués. Merci de votre confiance.");
                  } catch (org.springframework.mail.MailException ignored) {
                  }
              for (String email :
                  java.util.List.of(s.getClient().getEmail(), s.getProvider().getUser().getEmail()))
                ws.convertAndSendToUser(
                    email,
                    "/queue/notifications",
                    Map.of("sessionId", e.id(), "message", e.message()));
            });
  }

  @Scheduled(fixedDelay = 10000)
  @Transactional
  public void signal() {
    for (Long id : db.queryForList("SELECT id FROM sessions WHERE status='EN_ROUTE'", Long.class))
      sessions
          .findById(id)
          .ifPresent(
              s -> {
                var last = store.last(id);
                long at =
                    last == null
                        ? s.getEnRouteAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        : last.receivedAt();
                if (System.currentTimeMillis() - at > config.getSignalLostAfterS() * 1000L
                    && store.get("lost", id) == null) {
                  store.set("lost", id, "1", 50400);
                  tracking.event(
                      s, "SIGNAL_LOST", "Signal perdu : la dernière position peut être ancienne.");
                }
              });
  }

  @Scheduled(fixedDelay = 3600000)
  public void purge() {
    db.update(
        "DELETE FROM tracking_events WHERE created_at < ?",
        LocalDateTime.now().minusDays(retention));
    db.update("DELETE FROM trip_shares WHERE expires_at < CURRENT_TIMESTAMP");
  }
}
