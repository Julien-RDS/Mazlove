package com.mazlove.backend.tracking;

import com.mazlove.backend.service.EmailService;
import java.net.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Durable single-recipient deliveries. Ambiguous SMTP outcomes are never retried automatically. */
@Service
public class SosMailQueue {
  private final JdbcTemplate db;
  private final EmailService mail;
  private final TransactionTemplate tx;

  public SosMailQueue(JdbcTemplate db, EmailService mail, PlatformTransactionManager manager) {
    this.db = db;
    this.mail = mail;
    this.tx = new TransactionTemplate(manager);
  }

  public void enqueue(long alert, String recipient, String body) {
    db.update(
        "INSERT INTO sos_mail_queue(alert_id,recipient,body) VALUES (?,?,?) ON"
            + " CONFLICT(alert_id,recipient) DO NOTHING",
        alert,
        recipient,
        body);
  }

  public Map<String, Object> delivery(long alert) {
    var counts =
        db.queryForMap(
            "SELECT count(*) FILTER(WHERE status='SENT') AS sent,count(*) FILTER(WHERE"
                + " status='PENDING' OR status='SENDING') AS queued,count(*) FILTER(WHERE"
                + " status='FAILED') AS failed,count(*) FILTER(WHERE status='UNKNOWN') AS uncertain"
                + " FROM sos_mail_queue WHERE alert_id=?",
            alert);
    return Map.of(
        "alertId",
        alert,
        "emailsSent",
        counts.get("sent"),
        "emailsQueued",
        counts.get("queued"),
        "emailsFailed",
        counts.get("failed"),
        "emailsUncertain",
        counts.get("uncertain"),
        "message",
        "Alerte enregistrée auprès de l’équipe. En cas de danger, appelez le 17 ou le 112. Les"
            + " secours ne sont pas contactés automatiquement.");
  }

  // Retry only when we know SMTP did not accept the message. A timeout/disconnect may follow
  // acceptance.
  static String outcome(Throwable error, int attempts) {
    if (error instanceof MailPreparationException) return "FAILED";
    boolean certain = error instanceof MailAuthenticationException;
    Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    for (Throwable e = error; e != null && seen.add(e); e = e.getCause())
      if (e instanceof ConnectException
          || e instanceof UnknownHostException
          || e instanceof NoRouteToHostException) certain = true;
    if (!certain) return "UNKNOWN";
    return attempts < 3 ? "PENDING" : "FAILED";
  }

  @Scheduled(fixedDelayString = "${tracking.sos-mail.interval-ms:5000}")
  public void drain() {
    // A process may die after SMTP accepts a message. Preserve uncertainty instead of duplicating
    // it.
    db.update(
        "UPDATE sos_mail_queue SET status='UNKNOWN',updated_at=CURRENT_TIMESTAMP WHERE"
            + " status='SENDING' AND updated_at<CURRENT_TIMESTAMP-INTERVAL '10 minutes'");
    for (int i = 0; i < 10; i++) {
      Map<String, Object> job =
          tx.execute(
              status -> {
                var rows =
                    db.queryForList(
                        "SELECT id,recipient,body,attempts FROM sos_mail_queue WHERE"
                            + " status='PENDING' AND next_attempt_at<=CURRENT_TIMESTAMP ORDER BY id"
                            + " LIMIT 1 FOR UPDATE SKIP LOCKED");
                if (rows.isEmpty()) return null;
                var row = rows.get(0);
                db.update(
                    "UPDATE sos_mail_queue SET"
                        + " status='SENDING',attempts=attempts+1,updated_at=CURRENT_TIMESTAMP WHERE"
                        + " id=?",
                    row.get("id"));
                return row;
              });
      if (job == null) return;
      String result = "SENT";
      int attempt = ((Number) job.get("attempts")).intValue() + 1;
      try {
        mail.sendText(
            job.get("recipient").toString(), "Alerte SOS Mazlove", job.get("body").toString());
      } catch (MailException e) {
        result = outcome(e, attempt);
      }
      db.update(
          "UPDATE sos_mail_queue SET"
              + " status=?,updated_at=CURRENT_TIMESTAMP,next_attempt_at=CURRENT_TIMESTAMP+(? *"
              + " INTERVAL '1 second') WHERE id=?",
          result,
          30 * attempt,
          job.get("id"));
    }
  }

  @Scheduled(fixedDelay = 3600000)
  public void purge() {
    db.update(
        "DELETE FROM sos_mail_queue WHERE status IN ('SENT','FAILED','UNKNOWN') AND"
            + " updated_at<CURRENT_TIMESTAMP-INTERVAL '7 days'");
  }
}
