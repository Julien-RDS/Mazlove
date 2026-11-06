package com.mazlove.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mazlove.backend.models.*;
import com.mazlove.backend.repository.ReviewRepository;
import org.junit.jupiter.api.Test;

class SessionEmailStatusTest {
  @Test
  void onlyTheViewersOwnEmailStatusIsExposed() {
    var session = mock(Session.class);
    var client = mock(User.class);
    var practitioner = mock(User.class);
    var provider = mock(Provider.class);
    when(client.getId()).thenReturn(1L);
    when(practitioner.getId()).thenReturn(2L);
    when(provider.getUser()).thenReturn(practitioner);
    when(session.getClient()).thenReturn(client);
    when(session.getProvider()).thenReturn(provider);
    when(session.isClientEmailUnconfirmed()).thenReturn(true);
    when(session.getStatus()).thenReturn(Session.Status.DONE);
    var mapper = new SessionMapper(mock(ReviewRepository.class));
    assertTrue(mapper.toDTO(session, client).isEmailUnconfirmed());
    assertFalse(mapper.toDTO(session, practitioner).isEmailUnconfirmed());
  }
}
