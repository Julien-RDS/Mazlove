package com.mazlove.backend.tracking;

import static org.mockito.Mockito.*;

import com.mazlove.backend.models.Provider;
import com.mazlove.backend.models.Session;
import com.mazlove.backend.models.User;
import com.mazlove.backend.repository.SessionRepository;
import com.mazlove.backend.service.EmailService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class TrackingJobsTest {
  @Test
  void failedCompletionEmailNotifiesOnlyItsRecipientAndOtherRecipientStillReceivesEmail() {
    var db = mock(JdbcTemplate.class);
    var repo = mock(SessionRepository.class);
    var email = mock(EmailService.class);
    var ws = mock(SimpMessagingTemplate.class);
    var client = mock(User.class);
    var practitioner = mock(User.class);
    var provider = mock(Provider.class);
    var session = mock(Session.class);
    when(client.getEmail()).thenReturn("client@example.test");
    when(practitioner.getEmail()).thenReturn("provider@example.test");
    when(provider.getUser()).thenReturn(practitioner);
    when(session.getClient()).thenReturn(client);
    when(session.getProvider()).thenReturn(provider);
    when(repo.findById(1L)).thenReturn(Optional.of(session));
    when(db.queryForList("SELECT status FROM sessions WHERE id=? FOR UPDATE", String.class, 1L))
        .thenReturn(List.of("DONE"));
    doThrow(new org.springframework.mail.MailSendException("test"))
        .when(email)
        .sendText(eq("client@example.test"), anyString(), anyString());
    var jobs =
        new TrackingJobs(
            email,
            db,
            mock(TrackingStore.class),
            repo,
            mock(TrackingService.class),
            new TrackingConfig(),
            ws);
    jobs.changed(new SessionLifecycle.Changed(1L, "DONE", "Séance terminée"));
    verify(ws)
        .convertAndSendToUser(
            eq("client@example.test"),
            eq("/queue/notifications"),
            argThat(
                payload ->
                    payload instanceof java.util.Map<?, ?> map
                        && String.valueOf(map.get("message"))
                            .contains("n’a pas pu être confirmé")));
    verify(email).sendText(eq("provider@example.test"), anyString(), anyString());
  }

  final JdbcTemplate db = mock(JdbcTemplate.class);
  final TrackingStore store = mock(TrackingStore.class);
  final SessionRepository sessions = mock(SessionRepository.class);
  final TrackingService tracking = mock(TrackingService.class);
  final TrackingJobs jobs =
      new TrackingJobs(
          mock(EmailService.class),
          db,
          store,
          sessions,
          tracking,
          new TrackingConfig(),
          mock(SimpMessagingTemplate.class));

  @Test
  void signalLostIsPublishedOnlyOnce() {
    Session s = new Session();
    s.setId(1L);
    s.setStatus(com.mazlove.backend.models.Session.Status.EN_ROUTE);
    s.setEnRouteAt(LocalDateTime.now().minusMinutes(2));
    when(db.queryForList("SELECT id FROM sessions WHERE status='EN_ROUTE' ORDER BY id", Long.class))
        .thenReturn(List.of(1L));
    when(sessions.lockById(1L)).thenReturn(Optional.of(s));
    when(store.get("lost", 1)).thenReturn(null, "1");
    jobs.signal();
    jobs.signal();
    verify(tracking).event(eq(s), eq("SIGNAL_LOST"), anyString());
  }

  @Test
  void arrivalImmediatelyErasesLocationAndRevokesLinks() {
    jobs.changed(new SessionLifecycle.Changed(1L, "ARRIVED", "Arrivé"));
    verify(store).clear(1L);
    verify(db).update("UPDATE trip_shares SET revoked=true WHERE session_id=?", 1L);
  }
}
