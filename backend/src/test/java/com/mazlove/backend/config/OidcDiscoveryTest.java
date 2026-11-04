package com.mazlove.backend.config;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

class OidcDiscoveryTest {
  @Test
  void silentProviderTimesOutAndConcurrentRequestDoesNotQueueBehindIt() throws Exception {
    var entered = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    var server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          entered.countDown();
          try {
            release.await(5, java.util.concurrent.TimeUnit.SECONDS);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          } finally {
            exchange.close();
          }
        });
    server.start();
    var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
    try {
      var env =
          new org.springframework.mock.env.MockEnvironment()
              .withProperty("auth.oidc.issuer", "http://127.0.0.1:" + server.getAddress().getPort())
              .withProperty("auth.oidc.client-id", "client")
              .withProperty("auth.oidc.client-secret", "secret");
      var clients =
          new BoundedOidcClients(
              new OidcSettings(env), "http://localhost", java.time.Duration.ofMillis(200));
      var first = executor.submit(() -> clients.findByRegistrationId("oidc"));
      assertTrue(entered.await(2, java.util.concurrent.TimeUnit.SECONDS));
      assertTimeoutPreemptively(
          java.time.Duration.ofSeconds(1),
          () ->
              assertThrows(
                  IllegalStateException.class, () -> clients.findByRegistrationId("oidc")));
      var failure =
          assertThrows(
              java.util.concurrent.ExecutionException.class,
              () -> first.get(2, java.util.concurrent.TimeUnit.SECONDS));
      assertInstanceOf(
          org.springframework.web.client.ResourceAccessException.class, failure.getCause());
    } finally {
      release.countDown();
      executor.shutdownNow();
      server.stop(0);
    }
  }

  @Test
  void unavailableIssuerDoesNotBlockStartupAndDiscoveryIsThrottled() throws Exception {
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
      assertThrows(RuntimeException.class, () -> clients.findByRegistrationId("oidc"));
      int count = requests.get();
      assertTrue(count > 0);
      assertThrows(RuntimeException.class, () -> clients.findByRegistrationId("oidc"));
      assertEquals(
          count, requests.get()); // temporisation après échec : pas de rafale de découvertes
    } finally {
      server.stop(0);
    }
  }
}
