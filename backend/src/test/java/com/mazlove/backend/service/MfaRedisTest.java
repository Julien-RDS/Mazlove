package com.mazlove.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mazlove.backend.dto.OtpDTO;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.server.ResponseStatusException;

/** Redis jetable dédié au processus de test, sans Docker ni connexion aux données applicatives. */
@EnabledIfEnvironmentVariable(named = "MAZLOVE_TEST_REDIS_SERVER", matches = ".+")
class MfaRedisTest {
  static Process server;
  static LettuceConnectionFactory factory;
  static StringRedisTemplate redis;
  String email;
  EmailService mail;
  MfaService mfa;

  @BeforeAll
  static void start() throws Exception {
    int port;
    try (var socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }
    server =
        new ProcessBuilder(
                System.getenv("MAZLOVE_TEST_REDIS_SERVER"),
                "--bind",
                "127.0.0.1",
                "--port",
                String.valueOf(port),
                "--save",
                "",
                "--appendonly",
                "no")
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start();
    factory = new LettuceConnectionFactory("127.0.0.1", port);
    factory.afterPropertiesSet();
    redis = new StringRedisTemplate(factory);
    for (int i = 0; i < 100; i++) {
      try (var connection = factory.getConnection()) {
        if ("PONG".equals(connection.ping())) return;
      } catch (org.springframework.data.redis.RedisConnectionFailureException e) {
        Thread.sleep(20);
      }
    }
    fail("Redis de test n'a pas démarré");
  }

  @AfterAll
  static void stop() throws Exception {
    if (factory != null) factory.destroy();
    if (server != null) {
      server.destroy();
      if (!server.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) server.destroyForcibly();
    }
  }

  @BeforeEach
  void prepare() {
    email = UUID.randomUUID() + "@example.test";
    mail = mock(EmailService.class);
    var env = new MockEnvironment().withProperty("mazlove.dev.fixed-otp", "true");
    env.setActiveProfiles("dev");
    mfa = new MfaService(redis, mail, false, true, env);
  }

  @Test
  void aCodeCanBeConsumedOnlyOnceEvenConcurrently() throws Exception {
    mfa.sendOtp(email);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var first = executor.submit(() -> mfa.verifyOtp(new OtpDTO(email, "123456")));
      var second = executor.submit(() -> mfa.verifyOtp(new OtpDTO(email, "123456")));
      assertEquals(1, (first.get() ? 1 : 0) + (second.get() ? 1 : 0));
      assertFalse(mfa.verifyOtp(new OtpDTO(email, "123456")));
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void resendPreservesAttemptsAndFiveWrongGuessesBlockFurtherIssuance() {
    mfa.sendOtp(email);
    for (int i = 0; i < 2; i++) assertFalse(mfa.verifyOtp(new OtpDTO(email, "000000")));
    redis.delete("otp-send:" + email); // simule la fin du délai de renvoi, sans attendre 30 s
    mfa.sendOtp(email);
    assertEquals("2", redis.opsForValue().get("otp-attempts:" + email));
    for (int i = 0; i < 3; i++) assertFalse(mfa.verifyOtp(new OtpDTO(email, "000000")));
    assertFalse(mfa.verifyOtp(new OtpDTO(email, "123456")));
    redis.delete("otp-send:" + email);
    assertEquals(
        429,
        assertThrows(ResponseStatusException.class, () -> mfa.sendOtp(email))
            .getStatusCode()
            .value());
    assertTrue(redis.getExpire("otp-attempts:" + email) > 0);
  }

  @Test
  void sendCooldownLimitsEmailsAndDoesNotReplaceTheExistingCode() {
    mfa.sendOtp(email);
    assertEquals(
        429,
        assertThrows(ResponseStatusException.class, () -> mfa.sendOtp(email))
            .getStatusCode()
            .value());
    verify(mail, times(1)).sendOtpEmail(email, "123456");
    assertTrue(mfa.verifyOtp(new OtpDTO(email, "123456")));
    assertTrue(redis.getExpire("otp-send:" + email) <= 30);
  }

  @Test
  void expiredCodeAndMissingCodeAreRejected() throws Exception {
    assertFalse(mfa.verifyOtp(new OtpDTO(email, "123456")));
    mfa.sendOtp(email);
    assertTrue(redis.getExpire("otp:" + email) <= 300);
    redis.expire("otp:" + email, Duration.ofMillis(1));
    Thread.sleep(20);
    assertFalse(mfa.verifyOtp(new OtpDTO(email, "123456")));
  }

  @Test
  void failedDeliveryInvalidatesTheCode() {
    doThrow(new MailSendException("test")).when(mail).sendOtpEmail(email, "123456");
    assertThrows(MailSendException.class, () -> mfa.sendOtp(email));
    assertNull(redis.opsForValue().get("otp:" + email));
    assertFalse(mfa.verifyOtp(new OtpDTO(email, "123456")));
  }
}
