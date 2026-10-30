package com.mazlove.backend.tracking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mazlove.backend.service.EmailService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.*;
import org.springframework.transaction.*;

class SosMailQueueTest {
  @Test
  void onlyCertainFailuresAreRetriedAndAttemptsAreBounded() {
    var refused = new MailSendException("Échec", new java.net.ConnectException());
    assertEquals("PENDING", SosMailQueue.outcome(refused, 1));
    assertEquals("PENDING", SosMailQueue.outcome(refused, 2));
    assertEquals("FAILED", SosMailQueue.outcome(refused, 3));
    assertEquals(
        "UNKNOWN",
        SosMailQueue.outcome(
            new MailSendException("Échec", new java.net.SocketTimeoutException()), 1));
    assertEquals("FAILED", SosMailQueue.outcome(new MailPreparationException("Échec"), 1));
  }

  @Test
  void workerCommitsClaimBeforeMailAndRecordsFailureWithoutThrowing() {
    var db = mock(JdbcTemplate.class);
    var mail = mock(EmailService.class);
    var tm = mock(PlatformTransactionManager.class);
    var status = mock(TransactionStatus.class);
    when(tm.getTransaction(any())).thenReturn(status);
    when(db.queryForList(startsWith("SELECT id,recipient")))
        .thenReturn(
            List.of(
                Map.of(
                    "id",
                    1L,
                    "recipient",
                    "contact@example.test",
                    "body",
                    "Alerte",
                    "attempts",
                    0)),
            List.of());
    doThrow(new MailSendException("Échec", new java.net.ConnectException()))
        .when(mail)
        .sendText(anyString(), anyString(), anyString());
    new SosMailQueue(db, mail, tm).drain();
    var order = inOrder(db, tm, mail);
    order.verify(db).update(contains("status='SENDING',attempts"), eq(1L));
    order.verify(tm).commit(status);
    order.verify(mail).sendText(anyString(), anyString(), anyString());
    order
        .verify(db)
        .update(startsWith("UPDATE sos_mail_queue SET status=?"), eq("PENDING"), eq(30), eq(1L));
  }

  @Test
  void noPendingJobNeverSends() {
    var db = mock(JdbcTemplate.class);
    var mail = mock(EmailService.class);
    var tm = mock(PlatformTransactionManager.class);
    when(db.queryForList(anyString())).thenReturn(List.of());
    new SosMailQueue(db, mail, tm).drain();
    verifyNoInteractions(mail);
  }
}
