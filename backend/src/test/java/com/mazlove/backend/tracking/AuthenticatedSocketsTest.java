package com.mazlove.backend.tracking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mazlove.backend.config.AuthenticatedSockets;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.*;

class AuthenticatedSocketsTest {
  @Test
  void expiryBlocksMessagesBeforeTheCleanupJobAndClosesSocket() throws Exception {
    var s = mock(WebSocketSession.class);
    when(s.getId()).thenReturn("s");
    var registry = new AuthenticatedSockets();
    registry.connected(s);
    assertFalse(registry.valid("s"));
    registry.authenticate("s", "p", System.currentTimeMillis() + 60000);
    assertTrue(registry.valid("s"));
    registry.authenticate("s", "p", System.currentTimeMillis() - 1);
    assertFalse(registry.valid("s"));
    registry.expire();
    verify(s).close(argThat(c -> c.getCode() == 1008));
  }

  @Test
  void revocationAffectsOnlyTheDeletedAccount() throws Exception {
    var registry = new AuthenticatedSockets();
    var a = mock(WebSocketSession.class);
    var b = mock(WebSocketSession.class);
    when(a.getId()).thenReturn("a");
    when(b.getId()).thenReturn("b");
    registry.connected(a);
    registry.connected(b);
    registry.authenticate("a", "alice", Long.MAX_VALUE);
    registry.authenticate("b", "sophie", Long.MAX_VALUE);
    registry.revokeUser("alice");
    assertFalse(registry.valid("a"));
    assertTrue(registry.valid("b"));
    verify(b, never()).close(any());
  }
}
