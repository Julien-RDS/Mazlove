package com.mazlove.backend.tracking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mazlove.backend.config.StompAuthChannelInterceptor;
import com.mazlove.backend.exception.ForbiddenException;
import com.mazlove.backend.service.JWTService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.security.core.userdetails.UserDetailsService;

class StompSecurityTest {
  SessionAccessPolicy access = mock(SessionAccessPolicy.class);
  StompAuthChannelInterceptor interceptor =
      new StompAuthChannelInterceptor(
          mock(JWTService.class), mock(UserDetailsService.class), access);

  private Message<byte[]> message(StompCommand command, String dest, boolean signed) {
    var a = StompHeaderAccessor.create(command);
    if (dest != null) a.setDestination(dest);
    if (signed) a.setUser(() -> "intrus@test.com");
    return MessageBuilder.createMessage(new byte[0], a.getMessageHeaders());
  }

  @Test
  void anonymousConnectDenied() {
    assertThrows(
        MessagingException.class,
        () -> interceptor.preSend(message(StompCommand.CONNECT, null, false), null));
  }

  @Test
  void strangerCannotSubscribe() {
    when(access.assertParticipant("intrus@test.com", 42L))
        .thenThrow(new ForbiddenException("Interdit"));
    assertThrows(
        ForbiddenException.class,
        () ->
            interceptor.preSend(message(StompCommand.SUBSCRIBE, "/topic/tracking.42", true), null));
  }

  @Test
  void rawQueueAndWildcardAndDirectTopicSendsDenied() {
    for (String d : new String[] {"/queue/notifications", "/topic/tracking.*", "/topic/providers"})
      assertThrows(
          MessagingException.class,
          () -> interceptor.preSend(message(StompCommand.SUBSCRIBE, d, true), null));
    assertThrows(
        MessagingException.class,
        () -> interceptor.preSend(message(StompCommand.SEND, "/topic/tracking.42", true), null));
  }
}
