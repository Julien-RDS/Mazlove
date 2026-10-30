package com.mazlove.backend.config;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

@Component
public class AuthenticatedSockets {
  private static class Entry {
    final WebSocketSession socket;
    volatile String email;
    volatile long expires;

    Entry(WebSocketSession socket) {
      this.socket = socket;
      expires = System.currentTimeMillis() + 10000;
    }
  }

  private final ConcurrentHashMap<String, Entry> sockets = new ConcurrentHashMap<>();

  public void connected(WebSocketSession s) {
    sockets.put(s.getId(), new Entry(s));
  }

  public void closed(String id) {
    sockets.remove(id);
  }

  public void authenticate(String id, String email, long expires) {
    Entry e = sockets.get(id);
    if (e == null) throw new org.springframework.messaging.MessagingException("Connexion expirée.");
    e.email = email;
    e.expires = expires;
  }

  public boolean valid(String id) {
    Entry e = id == null ? null : sockets.get(id);
    return e != null && e.email != null && e.expires > System.currentTimeMillis();
  }

  private void close(Entry e) {
    try {
      e.socket.close(new CloseStatus(1008, "Session expirée ou révoquée"));
    } catch (java.io.IOException ignored) {
    } finally {
      sockets.remove(e.socket.getId(), e);
    }
  }

  public void revokeUser(String email) {
    sockets.values().stream().filter(e -> email.equals(e.email)).forEach(this::close);
  }

  @Scheduled(fixedDelay = 1000)
  public void expire() {
    sockets.values().stream()
        .filter(e -> e.expires <= System.currentTimeMillis())
        .forEach(this::close);
  }
}
