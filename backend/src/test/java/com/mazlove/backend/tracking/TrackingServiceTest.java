package com.mazlove.backend.tracking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mazlove.backend.exception.ForbiddenException;
import com.mazlove.backend.models.*;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class TrackingServiceTest {
  SessionAccessPolicy access = mock(SessionAccessPolicy.class);
  TrackingStore store = mock(TrackingStore.class);
  RoutingService routing = new StraightLineRoutingService();
  SessionLifecycle life = mock(SessionLifecycle.class);
  SimpMessagingTemplate ws = mock(SimpMessagingTemplate.class);
  JdbcTemplate db = mock(JdbcTemplate.class);
  TrackingService service =
      new TrackingService(
          access,
          store,
          routing,
          new TrackingConfig(),
          life,
          ws,
          db,
          mock(TrackingPublisher.class));
  Session session;

  @BeforeEach
  void setup() {
    var user = new User();
    user.setEmail("client@test.com");
    session = new Session();
    session.setId(1L);
    session.setStatus(Session.Status.EN_ROUTE);
    session.setClient(user);
    session.setMeetingLatitude(48.8584);
    session.setMeetingLongitude(2.347);
    session.setTravelMode(TravelMode.DRIVING);
    when(access.lockProvider("p", 1L)).thenReturn(session);
    when(store.rate(1)).thenReturn(true);
  }

  PositionDTO p(double lat, double lng, double accuracy, long seq) {
    return new PositionDTO(lat, lng, accuracy, null, 5., System.currentTimeMillis(), seq);
  }

  @Test
  void rejectsPoorAccuracyAndInvalidCoordinates() {
    service.ingest("p", 1, p(48, 2, 101, 1));
    service.ingest("p", 1, p(Double.NaN, 2, 5, 2));
    verify(store, never()).save(anyLong(), any());
  }

  @Test
  void clientCannotSend() {
    when(access.lockProvider("client", 1L))
        .thenThrow(new ForbiddenException("Prestataire uniquement"));
    assertThrows(ForbiddenException.class, () -> service.ingest("client", 1, p(48, 2, 5, 1)));
  }

  @Test
  void rejectsOutOfOrderAndTeleport() {
    when(store.last(1))
        .thenReturn(
            new TrackingStore.Last(
                new PositionDTO(
                    48.8584, 2.347, 5., null, 5., System.currentTimeMillis() - 1500, 10),
                System.currentTimeMillis() - 1500,
                5,
                5,
                true));
    service.ingest("p", 1, p(48.8584, 2.347, 5, 9));
    service.ingest("p", 1, p(40, 1, 5, 11));
    verify(store, never()).save(anyLong(), any());
  }

  @Test
  void rateLimit() {
    when(store.rate(1)).thenReturn(false);
    service.ingest("p", 1, p(48.8584, 2.347, 5, 1));
    verify(store, never()).save(anyLong(), any());
  }

  @Test
  void requiresTwoNearPings() {
    when(store.increment("near", 1, 60)).thenReturn(1L, 2L);
    service.ingest("p", 1, p(48.8584, 2.347, 5, 1));
    verifyNoInteractions(life);
    service.ingest("p", 1, p(48.8584, 2.347, 5, 2));
    verify(life).move(session, SessionStateMachine.Action.ARRIVE, SessionStateMachine.Actor.SYSTEM);
  }

  @Test
  void recoveryOnlyOnce() {
    when(store.get("lost", 1)).thenReturn("1", null);
    service.ingest("p", 1, p(48.85, 2.3, 5, 1));
    service.ingest("p", 1, p(48.85, 2.3, 5, 2));
    verify(store).delete("lost", 1);
  }

  @Test
  void deviationOnlyOncePerEpisode() {
    var r =
        new RoutingService.Route(
            List.of(new double[] {2.347, 48.8584}, new double[] {2.35, 48.8584}), 300, 30, false);
    when(store.route(1)).thenReturn(new TrackingStore.CachedRoute(r, System.currentTimeMillis()));
    when(store.get("off", 1)).thenReturn(Long.toString(System.currentTimeMillis() - 61000));
    when(store.get("off-alert", 1)).thenReturn(null, "1");
    service.ingest("p", 1, p(48.87, 2.347, 5, 1));
    service.ingest("p", 1, p(48.87, 2.347, 5, 2));
    verify(store).set("off-alert", 1, "1", 300);
  }
}
