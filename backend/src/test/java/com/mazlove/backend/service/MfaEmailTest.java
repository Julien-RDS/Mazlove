package com.mazlove.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;

class MfaEmailTest {
  @Test
  void fixedOtpRemainsAvailableWhenEnabledInDev() {
    var redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    ValueOperations<String, String> values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    when(redis.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(1L);
    var email = mock(EmailService.class);
    var env = new MockEnvironment().withProperty("mazlove.dev.fixed-otp", "true");
    env.setActiveProfiles("dev");
    new MfaService(redis, email, false, true, env).sendOtp("alice@example.test");
    verify(email).sendOtpEmail("alice@example.test", "123456");
  }

  @Test
  void disabledTwoFactorDoesNotSendEvenWithFixedOtpConfigured() {
    var redis = mock(StringRedisTemplate.class);
    var email = mock(EmailService.class);
    var env = new MockEnvironment().withProperty("mazlove.dev.fixed-otp", "true");
    env.setActiveProfiles("dev");
    new MfaService(redis, email, false, false, env).sendOtp("alice@example.test");
    verifyNoInteractions(redis, email);
  }

  @Test
  void sendsExactlyTheCodeStoredInRedisToTheNormalizedAddress() {
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    ValueOperations<String, String> values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    when(redis.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(1L);
    EmailService email = mock(EmailService.class);
    MfaService service = new MfaService(redis, email, false, true, new MockEnvironment());

    service.sendOtp("  Alice@Example.com  ");

    ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
    verify(redis)
        .execute(
            any(RedisScript.class),
            eq(
                List.of(
                    "otp:alice@example.com",
                    "otp-attempts:alice@example.com",
                    "otp-send:alice@example.com")),
            code.capture());
    assertTrue(code.getValue().matches("[0-9a-f]{64}:[0-9]{6}"));
    verify(email).sendOtpEmail("alice@example.com", code.getValue().substring(65));
  }

  @Test
  void deliveryFailureIsNotReportedAsSuccess() {
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    ValueOperations<String, String> values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    when(redis.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(1L);
    EmailService email = mock(EmailService.class);
    doThrow(new MailSendException("SMTP indisponible"))
        .when(email)
        .sendOtpEmail(anyString(), anyString());

    assertThrows(
        MailSendException.class,
        () ->
            new MfaService(redis, email, false, true, new MockEnvironment())
                .sendOtp("a@example.com"));
  }

  @Test
  void buildsAnEmailWithTheCodeAndExpiry() throws Exception {
    JavaMailSender sender = mock(JavaMailSender.class);
    MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
    when(sender.createMimeMessage()).thenReturn(message);
    EmailService service = new EmailService(sender, "no-reply@mazlove.fr");

    service.sendOtpEmail("alice@example.com", "012345");

    assertEquals("Votre code Mazlove : 012345", message.getSubject());
    assertEquals("alice@example.com", message.getAllRecipients()[0].toString());
    assertTrue(message.getContent().toString().contains("012345"));
    assertTrue(message.getContent().toString().contains("5 minutes"));
    verify(sender).send(message);
  }
}
