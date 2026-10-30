package com.mazlove.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mazlove.backend.exception.BadRequestException;
import com.mazlove.backend.exception.ForbiddenException;
import com.mazlove.backend.models.Provider;
import com.mazlove.backend.models.Session;
import com.mazlove.backend.models.User;
import com.mazlove.backend.repository.ProviderRepository;
import com.mazlove.backend.repository.ServiceItemRepository;
import com.mazlove.backend.repository.SessionRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionCancellationTest {
  @Mock SessionRepository sessions;
  @Mock ProviderRepository providers;
  @Mock AuthenticatedUserService auth;
  @Mock SessionMapper mapper;
  @Mock NotificationService notifications;
  @Mock ServiceItemRepository items;
  SessionService service;
  User client;
  Session session;

  @BeforeEach
  void setup() {
    service =
        new SessionService(
            sessions,
            providers,
            auth,
            mapper,
            notifications,
            items,
            new com.mazlove.backend.tracking.SessionLifecycle(
                sessions,
                mock(org.springframework.jdbc.core.JdbcTemplate.class),
                mock(org.springframework.context.ApplicationEventPublisher.class)));
    client = new User();
    client.setId(1L);
    client.setName("Client");
    User practitioner = new User();
    practitioner.setId(2L);
    practitioner.setEmail("p@example.org");
    Provider provider = new Provider();
    provider.setId(10L);
    provider.setUser(practitioner);
    session = new Session();
    session.setId(42L);
    session.setClient(client);
    session.setProvider(provider);
    session.setStatus(Session.Status.PENDING);
    when(sessions.findById(42L)).thenReturn(Optional.of(session));
  }

  @Test
  void refusesCancellationWithin24Hours() {
    when(auth.requireUser()).thenReturn(client);
    session.setDatetime(LocalDateTime.now().plusHours(23));
    assertThrows(BadRequestException.class, () -> service.cancel(42L));
    verify(sessions, never()).save(any());
  }

  @Test
  void cancelsAndNotifiesOtherParticipantWhenAllowed() {
    when(auth.requireUser()).thenReturn(client);
    session.setDatetime(LocalDateTime.now().plusHours(25));
    service.cancel(42L);
    assertEquals(Session.Status.CANCELLED, session.getStatus());
    verify(sessions).save(session);
    verify(notifications).notifySession(eq("p@example.org"), any());
  }

  @Test
  void rejectsSomeoneOutsideTheSession() {
    User stranger = new User();
    stranger.setId(3L);
    when(auth.requireUser()).thenReturn(stranger);
    assertThrows(ForbiddenException.class, () -> service.cancel(42L));
    verify(sessions, never()).save(any());
  }
}
