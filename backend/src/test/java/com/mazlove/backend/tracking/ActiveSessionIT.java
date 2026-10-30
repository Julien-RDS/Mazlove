package com.mazlove.backend.tracking;

import static org.junit.jupiter.api.Assertions.*;

import com.mazlove.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Real PostgreSQL query, isolated rows rolled back after each case. Use a disposable test database.
 */
@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "MAZLOVE_STOMP_IT", matches = "true")
class ActiveSessionIT {
  @Autowired JdbcTemplate db;
  @Autowired UserRepository users;
  @Autowired EntityManager em;
  long client, provider, user, stranger;
  LocalDateTime now = LocalDateTime.now();

  long user(String role) {
    return db.queryForObject(
        "INSERT INTO users(email,name,password,phone,role) VALUES"
            + " (?,'Test','unused','0000000000',?) RETURNING id",
        Long.class,
        UUID.randomUUID() + "@example.test",
        role);
  }

  @BeforeEach
  void setup() {
    client = user("CLIENT");
    user = user("PROVIDER");
    stranger = user("CLIENT");
    provider =
        db.queryForObject(
            "INSERT INTO providers(user_id,bio,category) VALUES (?,'Test','Test') RETURNING id",
            Long.class,
            user);
  }

  long session(String status, LocalDateTime at) {
    return db.queryForObject(
        "INSERT INTO sessions(client_id,provider_id,datetime,duration,status,code_validation)"
            + " VALUES (?,?,?,1,?,'123456') RETURNING id",
        Long.class,
        client,
        provider,
        at,
        status);
  }

  long active(long who, boolean only) {
    em.clear();
    return users.findActiveSession(who, only, now).orElseThrow().getId();
  }

  @Test
  void findsLiveTripBeyondFiftyBookingsAndBothRoles() {
    for (int i = 1; i <= 61; i++) session("PENDING", now.plusDays(i));
    long live = session("EN_ROUTE", now.minusHours(2));
    assertEquals(live, active(client, false));
    assertEquals(live, active(user, false));
    assertEquals(live, active(user, true));
    assertTrue(users.findActiveSession(stranger, false, now).isEmpty());
    assertTrue(users.findActiveSession(client, true, now).isEmpty());
  }

  @Test
  void followsStatePriorityThenNearestReservation() {
    long next = session("ACCEPTED", now.plusHours(3));
    session("PENDING", now.plusHours(9));
    long progress = session("IN_PROGRESS", now.minusHours(4));
    long arrived = session("ARRIVED", now.minusHours(3));
    long live = session("EN_ROUTE", now.minusHours(2));
    for (long expected : new long[] {live, arrived, progress, next}) {
      assertEquals(expected, active(client, false));
      db.update("UPDATE sessions SET status='DONE' WHERE id=?", expected);
    }
  }

  @Test
  void excludesOldAndTerminalReservations() {
    session("ACCEPTED", now.minusDays(1));
    session("PENDING", now.minusDays(2));
    session("CANCELLED", now.plusDays(1));
    session("DONE", now.plusDays(2));
    assertTrue(users.findActiveSession(client, false, now).isEmpty());
  }
}
