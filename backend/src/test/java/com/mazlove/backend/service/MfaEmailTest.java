package com.mazlove.backend.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Duration;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MfaEmailTest {
    @Test
    void sendsExactlyTheCodeStoredInRedisToTheNormalizedAddress() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        EmailService email = mock(EmailService.class);
        MfaService service = new MfaService(redis, email, false);

        service.sendOtp("  Alice@Example.com  ");

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(values).set(eq("otp:alice@example.com"), code.capture(), eq(Duration.ofMinutes(5)));
        assertTrue(code.getValue().matches("[0-9]{6}"));
        verify(email).sendOtpEmail("alice@example.com", code.getValue());
    }

    @Test
    void deliveryFailureIsNotReportedAsSuccess() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        EmailService email = mock(EmailService.class);
        doThrow(new MailSendException("SMTP indisponible")).when(email).sendOtpEmail(anyString(), anyString());

        assertThrows(MailSendException.class, () -> new MfaService(redis, email, false).sendOtp("a@example.com"));
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
