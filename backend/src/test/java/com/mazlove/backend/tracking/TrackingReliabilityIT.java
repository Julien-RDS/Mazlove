package com.mazlove.backend.tracking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mazlove.backend.service.EmailService;
import com.mazlove.backend.service.JWTService;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.*;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Requires disposable PostgreSQL + Redis. Fixture routing never calls a remote router. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.sql.init.data-locations=classpath:sql/seed.sql",
      "tracking.sos-mail.interval-ms=3600000"
    })
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "MAZLOVE_STOMP_IT", matches = "true")
class TrackingReliabilityIT {
  @Autowired TestRestTemplate http;
  @Autowired JWTService jwt;
  @Autowired JdbcTemplate db;
  @Autowired TrackingService tracking;
  @Autowired TrackingStore store;
  @Autowired TrackingJobs jobs;
  @Autowired StringRedisTemplate redis;
  @Autowired SosMailQueue mailQueue;

  @MockBean(name = "resilientRoutingService")
  RoutingService routing;

  @SpyBean EmailService mail;
  long id;
  RoutingService.Route route;
  String contact;

  ResponseEntity<Map> request(String path, HttpMethod method, String who, Object body) {
    var h = new HttpHeaders();
    if (who != null) h.setBearerAuth(jwt.generateToken(who));
    return http.exchange(path, method, new HttpEntity<>(body, h), Map.class);
  }

  Map post(String path, String who, Object body) {
    var r = request(path, HttpMethod.POST, who, body);
    assertTrue(r.getStatusCode().is2xxSuccessful(), "Statut inattendu : " + r.getStatusCode());
    return r.getBody();
  }

  @BeforeEach
  void setup() throws Exception {
    Path file = Path.of("scripts/routes/chatelet.geojson");
    if (!Files.exists(file)) file = Path.of("../scripts/routes/chatelet.geojson");
    var fixture = new ObjectMapper().readTree(file.toFile());
    List<double[]> coordinates = new ArrayList<>();
    fixture
        .path("geometry")
        .path("coordinates")
        .forEach(n -> coordinates.add(new double[] {n.get(0).asDouble(), n.get(1).asDouble()}));
    route =
        new RoutingService.Route(
            coordinates,
            fixture.path("properties").path("distance").asDouble(),
            fixture.path("properties").path("duration").asDouble(),
            false);
    when(routing.route(any(), any(), any())).thenReturn(route);
    id = ((Number) post("/api/dev/reset-demo", "sophie@test.com", null).get("id")).longValue();
    db.update("DELETE FROM sos_alerts WHERE session_id=?", id);
    contact = "sos-" + UUID.randomUUID() + "@example.test";
    post("/api/consents", "sophie@test.com", null);
    post(
        "/api/sessions/" + id + "/depart",
        "sophie@test.com",
        Map.of(
            "travelMode",
            "DRIVING",
            "lat",
            route.coordinates().get(0)[1],
            "lng",
            route.coordinates().get(0)[0]));
  }

  @AfterEach
  void cleanup() {
    db.update("DELETE FROM trusted_contacts WHERE email=?", contact);
    db.update("DELETE FROM sos_alerts WHERE session_id=?", id);
    reset(mail);
    post("/api/dev/reset-demo", "sophie@test.com", null);
  }

  PositionDTO point(int index, long ts, long seq, double accuracy) {
    var c = route.coordinates().get(index);
    return new PositionDTO(c[1], c[0], accuracy, null, 0., ts, seq);
  }

  void ingest(PositionDTO p) {
    redis.delete("track:rate:" + id);
    tracking.ingest("sophie@test.com", id, p);
  }

  @Test
  void filtersEtaTtlLossRecoveryAndTwoPingArrival() {
    long now = System.currentTimeMillis();
    var first = point(0, now - 20000, 1, 5);
    ingest(first);
    var initial = store.last(id);
    assertNotNull(initial);
    assertTrue(redis.getExpire("track:last:" + id) > 0);
    assertTrue(redis.getExpire("track:route:" + id) > 0);
    ingest(point(1, now - 10000, 2, 150));
    assertEquals(1, store.last(id).point().seq());
    ingest(point(1, now - 10000, 0, 5));
    assertEquals(1, store.last(id).point().seq());
    ingest(new PositionDTO(40, 1, 5., null, 0., now - 10000, 2));
    assertEquals(1, store.last(id).point().seq());
    ingest(point(3, now - 10000, 2, 5));
    assertTrue(store.last(id).etaSeconds() < initial.etaSeconds());
    var last = store.last(id);
    store.save(
        id,
        new TrackingStore.Last(
            last.point(), now - 60000, last.etaSeconds(), last.distanceMeters(), false));
    jobs.signal();
    assertEquals("1", store.get("lost", id));
    ingest(point(4, System.currentTimeMillis(), 3, 5));
    assertNull(store.get("lost", id));
    store.clear(id);
    var end = new PositionDTO(48.8584, 2.347, 5., null, 0., System.currentTimeMillis() - 10000, 10);
    ingest(end);
    assertEquals(
        "EN_ROUTE", db.queryForObject("SELECT status FROM sessions WHERE id=?", String.class, id));
    ingest(new PositionDTO(end.lat(), end.lng(), 5., null, 0., System.currentTimeMillis(), 11));
    assertEquals(
        "ARRIVED", db.queryForObject("SELECT status FROM sessions WHERE id=?", String.class, id));
    assertNull(store.last(id));
    assertNull(store.route(id));
  }

  @Test
  void arrivalRacingWithGpsCannotLeaveALivePoint() throws Exception {
    var pool = Executors.newFixedThreadPool(2);
    var gate = new CountDownLatch(1);
    try {
      var a =
          pool.submit(
              () -> {
                try {
                  gate.await();
                  ingest(point(0, System.currentTimeMillis(), 1, 5));
                } catch (InterruptedException e) {
                  throw new RuntimeException(e);
                }
              });
      var b =
          pool.submit(
              () -> {
                try {
                  gate.await();
                  post("/api/sessions/" + id + "/arrived", "sophie@test.com", null);
                } catch (InterruptedException e) {
                  throw new RuntimeException(e);
                }
              });
      gate.countDown();
      a.get(10, TimeUnit.SECONDS);
      b.get(10, TimeUnit.SECONDS);
      assertNull(store.last(id));
      assertNull(store.route(id));
      ingest(point(1, System.currentTimeMillis(), 2, 5));
      assertNull(store.last(id));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void sharesExpireRevokeAndRateLimitHasATtl() {
    String url =
        post("/api/sessions/" + id + "/share", "alice@test.com", null).get("url").toString();
    String token = url.substring(url.lastIndexOf('/') + 1);
    String path = "/api/public/trips/" + token;
    assertEquals(200, request(path, HttpMethod.GET, null, null).getStatusCode().value());
    db.update(
        "UPDATE trip_shares SET expires_at=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE token=?",
        token);
    assertEquals(404, request(path, HttpMethod.GET, null, null).getStatusCode().value());
    db.update(
        "UPDATE trip_shares SET expires_at=CURRENT_TIMESTAMP+INTERVAL '1 hour' WHERE token=?",
        token);
    request("/api/sessions/" + id + "/share", HttpMethod.DELETE, "alice@test.com", null);
    assertEquals(404, request(path, HttpMethod.GET, null, null).getStatusCode().value());
    boolean limited = false;
    for (int i = 0; i < 31; i++)
      limited |= request(path, HttpMethod.GET, null, null).getStatusCode().value() == 429;
    assertTrue(limited);
    var keys = redis.keys("public-trip:*");
    assertFalse(keys.isEmpty());
    for (String key : keys) assertTrue(redis.getExpire(key) > 0);
  }

  @Test
  void sosPersistsAndIsDeduplicatedWhileSmtpFails() {
    post(
        "/api/users/me/trusted-contacts",
        "alice@test.com",
        Map.of("name", "Test", "email", contact));
    doThrow(
            new org.springframework.mail.MailSendException(
                "Échec", new java.net.ConnectException()))
        .when(mail)
        .sendText(eq(contact), anyString(), anyString());
    var first = post("/api/sessions/" + id + "/sos", "alice@test.com", null);
    var again = post("/api/sessions/" + id + "/sos", "alice@test.com", null);
    assertEquals(first.get("alertId"), again.get("alertId"));
    long alert = ((Number) first.get("alertId")).longValue();
    mailQueue.drain();
    assertEquals(
        1,
        db.queryForObject(
            "SELECT attempts FROM sos_mail_queue WHERE alert_id=? AND recipient=?",
            Integer.class,
            alert,
            contact));
    for (int i = 0; i < 2; i++) {
      db.update(
          "UPDATE sos_mail_queue SET next_attempt_at=CURRENT_TIMESTAMP WHERE alert_id=?", alert);
      mailQueue.drain();
    }
    assertEquals(
        "FAILED",
        db.queryForObject(
            "SELECT status FROM sos_mail_queue WHERE alert_id=? AND recipient=?",
            String.class,
            alert,
            contact));
    assertEquals(
        "OPEN", db.queryForObject("SELECT status FROM sos_alerts WHERE id=?", String.class, alert));
    verify(mail, times(3)).sendText(eq(contact), anyString(), anyString());
    assertEquals(
        403,
        request("/api/admin/sos", HttpMethod.GET, "alice@test.com", null).getStatusCode().value());
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "MAZLOVE_MAIL_IT", matches = "true")
  void emailIsReceivedInMailHogAndAdminCanReadAlerts() {
    post(
        "/api/users/me/trusted-contacts",
        "alice@test.com",
        Map.of("name", "Test", "email", contact));
    post("/api/sessions/" + id + "/sos", "alice@test.com", null);
    mailQueue.drain();
    String base = System.getenv().getOrDefault("MAILHOG_URL", "http://localhost:8025");
    var inbox =
        new org.springframework.web.client.RestTemplate()
            .getForObject(base + "/api/v2/search?kind=to&query=" + contact, Map.class);
    assertEquals(1, ((Number) inbox.get("total")).intValue());
    String admin = "admin-" + UUID.randomUUID() + "@example.test";
    db.update(
        "INSERT INTO users(email,name,password,phone,role) VALUES"
            + " (?,'Test','unused','0000000000','ADMIN')",
        admin);
    try {
      var h = new HttpHeaders();
      h.setBearerAuth(jwt.generateToken(admin));
      assertEquals(
          200,
          http.exchange("/api/admin/sos", HttpMethod.GET, new HttpEntity<>(h), List.class)
              .getStatusCode()
              .value());
    } finally {
      db.update("DELETE FROM users WHERE email=?", admin);
    }
  }

  @Test
  void departureFreezesMeetingPointAndOnlyOneTripMayBeEnRoute() {
    for (String path : List.of("/api/users/me/active-session", "/api/users/me/provider-trip")) {
      var active = request(path, HttpMethod.GET, "sophie@test.com", null);
      assertEquals(200, active.getStatusCode().value());
      assertEquals(id, ((Number) active.getBody().get("id")).longValue());
      assertEquals("PROVIDER", active.getBody().get("viewerRole"));
    }
    var client = request("/api/users/me/active-session", HttpMethod.GET, "alice@test.com", null);
    assertEquals(200, client.getStatusCode().value());
    assertEquals(id, ((Number) client.getBody().get("id")).longValue());
    assertEquals("CLIENT", client.getBody().get("viewerRole"));
    var changed =
        request(
            "/api/sessions/" + id + "/meeting-point",
            HttpMethod.PUT,
            "alice@test.com",
            Map.of("lat", 48.86, "lng", 2.35, "label", "Autre rendez-vous"));
    assertEquals(409, changed.getStatusCode().value());
    Long other =
        db.queryForObject(
            "INSERT INTO"
                + " sessions(client_id,provider_id,datetime,duration,status,code_validation,meeting_latitude,meeting_longitude)"
                + " SELECT client_id,provider_id,datetime+INTERVAL '1"
                + " day',1,'ACCEPTED','123456',meeting_latitude,meeting_longitude FROM sessions"
                + " WHERE id=? RETURNING id",
            Long.class,
            id);
    try {
      var second =
          request(
              "/api/sessions/" + other + "/depart",
              HttpMethod.POST,
              "sophie@test.com",
              Map.of("travelMode", "DRIVING", "lat", 48.87, "lng", 2.32));
      assertEquals(409, second.getStatusCode().value());
      assertEquals(
          1,
          db.queryForObject(
              "SELECT count(*) FROM sessions WHERE provider_id=(SELECT provider_id FROM sessions"
                  + " WHERE id=?) AND status='EN_ROUTE'",
              Integer.class,
              id));
    } finally {
      db.update("DELETE FROM sessions WHERE id=?", other);
    }
  }
}
