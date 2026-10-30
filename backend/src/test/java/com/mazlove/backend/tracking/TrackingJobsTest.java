package com.mazlove.backend.tracking;

import static org.mockito.Mockito.*;

import com.mazlove.backend.models.Session;
import com.mazlove.backend.repository.SessionRepository;
import com.mazlove.backend.service.EmailService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class TrackingJobsTest {
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
