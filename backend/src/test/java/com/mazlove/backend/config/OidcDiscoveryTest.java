package com.mazlove.backend.config;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

class OidcDiscoveryTest {
  @Test
  void unavailableIssuerDoesNotBlockStartupAndFailedDiscoveryCanRetry() throws Exception {
    var requests = new AtomicInteger();
    var server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          requests.incrementAndGet();
          exchange.sendResponseHeaders(503, -1);
          exchange.close();
        });
    server.start();
    try (var context = new AnnotationConfigApplicationContext()) {
      context
          .getEnvironment()
          .getPropertySources()
          .addFirst(
              new org.springframework.core.env.MapPropertySource(
                  "oidc",
                  java.util.Map.of(
                      "auth.oidc.issuer",
                      "http://127.0.0.1:" + server.getAddress().getPort(),
                      "auth.oidc.client-id",
                      "test-client",
                      "auth.oidc.client-secret",
                      "test-secret")));
      context.register(OidcSettings.class, OidcClientConfiguration.class);
      context.refresh();
      assertEquals(0, requests.get());
      var clients = context.getBean(ClientRegistrationRepository.class);
      assertNull(clients.findByRegistrationId("unknown"));
      assertThrows(IllegalArgumentException.class, () -> clients.findByRegistrationId("oidc"));
      int count = requests.get();
      assertTrue(count > 0);
      assertThrows(IllegalArgumentException.class, () -> clients.findByRegistrationId("oidc"));
      assertTrue(requests.get() > count);
    } finally {
      server.stop(0);
    }
  }
}
