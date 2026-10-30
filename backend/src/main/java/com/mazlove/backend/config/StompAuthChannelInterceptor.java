package com.mazlove.backend.config;

import com.mazlove.backend.service.JWTService;
import com.mazlove.backend.tracking.SessionAccessPolicy;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {
  private final JWTService jwt;
  private final UserDetailsService users;
  private final SessionAccessPolicy access;
  private static final Pattern PRIVATE =
      Pattern.compile("/topic/(?:tracking\\.(\\d+)|session\\.(\\d+)\\.status)");

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor a = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (a == null || a.getCommand() == null) return message;
    switch (a.getCommand()) {
      case CONNECT -> {
        String h = a.getFirstNativeHeader("Authorization");
        if (h == null || !h.startsWith("Bearer "))
          throw new MessagingException("Connexion authentifiée requise.");
        String email =
            jwt.validateAndGetSubject(h.substring(7))
                .orElseThrow(() -> new MessagingException("Jeton invalide ou expiré."));
        var u = users.loadUserByUsername(email);
        a.setUser(new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
      }
      case SUBSCRIBE -> {
        if (a.getUser() == null) throw new MessagingException("Connexion authentifiée requise.");
        String d = a.getDestination();
        if (d == null) throw new MessagingException("Destination manquante.");
        var m = PRIVATE.matcher(d);
        if (m.matches())
          access.assertParticipant(
              a.getUser().getName(), Long.valueOf(m.group(1) != null ? m.group(1) : m.group(2)));
        else if (d.equals("/topic/admin.alerts")) {
          if (!(a.getUser() instanceof Authentication auth)
              || auth.getAuthorities().stream()
                  .noneMatch(x -> x.getAuthority().equals("ROLE_ADMIN")))
            throw new MessagingException("Accès réservé à l'équipe.");
        } else if (!d.equals("/user/queue/notifications")
            && !d.equals("/user/queue/errors")
            && !d.equals("/user/queue/sessions"))
          throw new MessagingException("Abonnement interdit.");
      }
      case SEND -> {
        if (a.getUser() == null
            || a.getDestination() == null
            || !a.getDestination().startsWith("/app/"))
          throw new MessagingException("Destination interdite.");
      }
      default -> {}
    }
    return message;
  }
}
