package com.mazlove.backend.tracking;

import static org.junit.jupiter.api.Assertions.*;

import com.mazlove.backend.service.JWTService;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.sql.init.data-locations=classpath:sql/seed.sql"})
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "MAZLOVE_STOMP_IT", matches = "true")
class StompSecurityIT {
  @LocalServerPort int port;
  @Autowired JWTService jwt;
  @Autowired TestRestTemplate http;
  @Autowired TrackingStore store;

  @org.springframework.beans.factory.annotation.Value("${jwt.secret}")
  String secret;

  @org.springframework.boot.test.mock.mockito.MockBean(name = "resilientRoutingService")
  RoutingService routing;

  WebSocketStompClient ws;
  List<StompSession> connections = new ArrayList<>();

  @BeforeEach
  void setup() {
    org.mockito.Mockito.when(
            routing.route(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
        .thenAnswer(
            call ->
                new StraightLineRoutingService()
                    .route(call.getArgument(0), call.getArgument(1), call.getArgument(2)));
    ws = new WebSocketStompClient(new StandardWebSocketClient());
    ws.setMessageConverter(new MappingJackson2MessageConverter());
  }

  @AfterEach
  void close() {
    connections.forEach(
        s -> {
          if (s.isConnected()) s.disconnect();
        });
    ws.stop();
  }

  StompSession connect(String email, StompSessionHandlerAdapter handler) throws Exception {
    StompHeaders h = new StompHeaders();
    if (email != null) h.add("Authorization", "Bearer " + jwt.generateToken(email));
    StompSession s =
        ws.connectAsync(
                "ws://localhost:" + port + "/ws",
                new org.springframework.web.socket.WebSocketHttpHeaders(),
                h,
                handler)
            .get(5, TimeUnit.SECONDS);
    connections.add(s);
    return s;
  }

  Map call(String path, String email, Object body) {
    HttpHeaders h = new HttpHeaders();
    h.setBearerAuth(jwt.generateToken(email));
    var r = http.exchange(path, HttpMethod.POST, new HttpEntity<>(body, h), Map.class);
    assertTrue(r.getStatusCode().is2xxSuccessful(), r.toString());
    return r.getBody();
  }

  StompFrameHandler handler(CountDownLatch latch) {
    return new StompFrameHandler() {
      public Type getPayloadType(StompHeaders h) {
        return Map.class;
      }

      public void handleFrame(StompHeaders h, Object payload) {
        latch.countDown();
      }
    };
  }

  @Test
  void authenticatedTransportAndParticipantIsolation() throws Exception {
    assertThrows(Exception.class, () -> connect(null, new StompSessionHandlerAdapter() {}));
    long id = ((Number) call("/api/dev/reset-demo", "sophie@test.com", null).get("id")).longValue();
    call("/api/consents", "sophie@test.com", null);
    call(
        "/api/sessions/" + id + "/depart",
        "sophie@test.com",
        Map.of("travelMode", "DRIVING", "lat", 48.87, "lng", 2.32));
    CountDownLatch denied = new CountDownLatch(1);
    StompSession stranger =
        connect(
            "bob@test.com",
            new StompSessionHandlerAdapter() {
              public Type getPayloadType(StompHeaders h) {
                return String.class;
              }

              public void handleFrame(StompHeaders h, Object p) {
                denied.countDown();
              }

              public void handleTransportError(StompSession s, Throwable e) {
                denied.countDown();
              }
            });
    stranger.subscribe("/topic/tracking." + id, handler(new CountDownLatch(1)));
    assertTrue(denied.await(5, TimeUnit.SECONDS));
    var client = connect("alice@test.com", new StompSessionHandlerAdapter() {});
    var provider = connect("sophie@test.com", new StompSessionHandlerAdapter() {});
    CountDownLatch received = new CountDownLatch(3), refused = new CountDownLatch(1);
    client.subscribe("/topic/tracking." + id, handler(received));
    client.subscribe("/user/queue/errors", handler(refused));
    Thread.sleep(300);
    client.send(
        "/app/tracking/" + id + "/position",
        new PositionDTO(48.87, 2.32, 5., null, 0., System.currentTimeMillis(), 1));
    assertTrue(refused.await(5, TimeUnit.SECONDS));
    for (int i = 0; i < 3; i++) {
      provider.send(
          "/app/tracking/" + id + "/position",
          new PositionDTO(
              48.87 + i * .00001, 2.32, 5., null, 1., System.currentTimeMillis(), i + 1));
      Thread.sleep(3000);
    }
    assertTrue(received.await(5, TimeUnit.SECONDS));
    call("/api/sessions/" + id + "/arrived", "sophie@test.com", null);
    assertNull(store.last(id));
    assertNull(store.route(id));
    CountDownLatch late = new CountDownLatch(1);
    client.subscribe("/topic/tracking." + id, handler(late));
    Thread.sleep(200);
    provider.send(
        "/app/tracking/" + id + "/position",
        new PositionDTO(48.87, 2.32, 5., null, 0., System.currentTimeMillis(), 100));
    assertFalse(late.await(1500, TimeUnit.MILLISECONDS));
  }

  @Test
  void expirationDuringAnOpenConnectionClosesTheSocket() throws Exception {
    CountDownLatch expired = new CountDownLatch(1);
    var shortJwt = new JWTService(secret, 3000);
    StompHeaders headers = new StompHeaders();
    headers.add("Authorization", "Bearer " + shortJwt.generateToken("alice@test.com"));
    StompSession s =
        ws.connectAsync(
                "ws://localhost:" + port + "/ws",
                new org.springframework.web.socket.WebSocketHttpHeaders(),
                headers,
                new StompSessionHandlerAdapter() {
                  @Override
                  public void handleTransportError(StompSession session, Throwable error) {
                    expired.countDown();
                  }
                })
            .get(5, TimeUnit.SECONDS);
    connections.add(s);
    s.subscribe("/user/queue/notifications", handler(new CountDownLatch(1)));
    assertTrue(expired.await(6, TimeUnit.SECONDS));
    assertFalse(s.isConnected());
  }
}
