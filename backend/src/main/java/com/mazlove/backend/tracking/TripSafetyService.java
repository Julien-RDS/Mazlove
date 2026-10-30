package com.mazlove.backend.tracking;

import com.mazlove.backend.exception.*;
import com.mazlove.backend.models.*;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TripSafetyService {
  private final SessionAccessPolicy access;
  private final JdbcTemplate db;
  private final TrackingService tracking;
  private final TrackingStore store;
  private final SimpMessagingTemplate ws;
  private final SosMailQueue mail;
  private final StringRedisTemplate redis;

  @Value("${tracking.public-base-url:http://localhost:3000}")
  String publicBase;

  @Value("${tracking.share.ttl-after-end-h:1}")
  int shareHours;

  public Map<String, Object> share(String who, long id) {
    Session s = access.lockParticipant(who, id);
    if (s.getStatus() != Session.Status.EN_ROUTE)
      throw new BadRequestException("Le partage est disponible uniquement pendant le trajet.");
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    LocalDateTime expires = s.getEndTime().plusHours(shareHours);
    if (expires.isBefore(LocalDateTime.now())) expires = LocalDateTime.now().plusHours(shareHours);
    db.update(
        "INSERT INTO trip_shares(token,session_id,created_by,expires_at) VALUES (?,?,?,?)",
        token,
        id,
        access.user(who).getId(),
        expires);
    return Map.of("url", publicBase + "/t/" + token, "expiresAt", expires);
  }

  public void revoke(String who, long id) {
    access.lockParticipant(who, id);
    db.update(
        "UPDATE trip_shares SET revoked=true WHERE session_id=? AND created_by=?",
        id,
        access.user(who).getId());
  }

  public Map<String, Object> publicTrip(String token, String ip) {
    String key = "public-trip:" + ip;
    Long n =
        redis.execute(
            new org.springframework.data.redis.core.script.DefaultRedisScript<Long>(
                "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],60)"
                    + " end; return n",
                Long.class),
            List.of(key));
    if (n != null && n > 30)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "Réessayez dans une minute.");
    List<Long> ids =
        db.queryForList(
            "SELECT session_id FROM trip_shares WHERE token=? AND revoked=false AND"
                + " expires_at>CURRENT_TIMESTAMP",
            Long.class,
            token);
    if (ids.isEmpty()) throw new NotFoundException("Ce partage a expiré ou a été arrêté.");
    var rows =
        db.queryForList(
            "SELECT s.status,u.name,u.avatar FROM sessions s JOIN providers p ON p.id=s.provider_id"
                + " JOIN users u ON u.id=p.user_id WHERE s.id=? FOR SHARE OF s",
            ids.get(0));
    if (rows.isEmpty()
        || Boolean.FALSE.equals(
            db.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM trip_shares WHERE token=? AND revoked=false AND"
                    + " expires_at>CURRENT_TIMESTAMP)",
                Boolean.class,
                token))) throw new NotFoundException("Ce partage a expiré ou a été arrêté.");
    var row = rows.get(0);
    if (!"EN_ROUTE".equals(row.get("status")))
      throw new NotFoundException("Ce trajet est terminé.");
    var last = store.last(ids.get(0));
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", row.get("status"));
    result.put("providerName", row.get("name").toString().split(" ")[0]);
    result.put("photo", row.get("avatar"));
    if (last != null) {
      var p = last.point();
      result.put(
          "pos",
          Map.of(
              "lat",
              Math.rint(p.lat() / 0.00018) * 0.00018,
              "lng",
              Math.rint(p.lng() / 0.00027) * 0.00027,
              "ts",
              last.receivedAt()));
      result.put("etaSeconds", last.etaSeconds());
      result.put("distanceMeters", last.distanceMeters());
    }
    return result;
  }

  public Map<String, Object> sos(String who, long id) {
    Session s = access.lockParticipant(who, id);
    if (!Set.of(Session.Status.EN_ROUTE, Session.Status.ARRIVED, Session.Status.IN_PROGRESS)
        .contains(s.getStatus()))
      throw new BadRequestException("L'alerte est disponible pendant le trajet et la séance.");
    User u = access.user(who);
    var existing =
        db.queryForList(
            "SELECT id FROM sos_alerts WHERE session_id=? AND raised_by=? AND status='OPEN' ORDER"
                + " BY id DESC LIMIT 1",
            Long.class,
            id,
            u.getId());
    if (!existing.isEmpty()) return mail.delivery(existing.get(0));
    var last = store.last(id);
    Long alert =
        db.queryForObject(
            "INSERT INTO sos_alerts(session_id,raised_by,latitude,longitude) VALUES (?,?,?,?)"
                + " RETURNING id",
            Long.class,
            id,
            u.getId(),
            last == null ? null : last.point().lat(),
            last == null ? null : last.point().lng());
    tracking.event(s, "SOS", "Une alerte a été déclenchée.");
    AfterCommit.run(
        () ->
            ws.convertAndSend(
                "/topic/admin.alerts", Map.of("sessionId", id, "message", "Nouvelle alerte SOS")));
    String link =
        s.getStatus() == Session.Status.EN_ROUTE
            ? share(who, id).get("url").toString()
            : publicBase;
    for (String to :
        db.queryForList(
            "SELECT DISTINCT lower(email) FROM trusted_contacts WHERE user_id=? AND email IS NOT"
                + " NULL AND trim(email)<>''",
            String.class,
            u.getId()))
      mail.enqueue(
          alert,
          to,
          u.getName()
              + " a déclenché une alerte. Contactez cette personne. Suivi : "
              + link
              + ". En cas de danger immédiat : 17 ou 112. Mazlove ne contacte pas les secours.");
    return mail.delivery(alert);
  }

  public Map<String, Object> delivery(String who, long session, long alert) {
    access.assertParticipant(who, session);
    if (!Boolean.TRUE.equals(
        db.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM sos_alerts WHERE id=? AND session_id=? AND raised_by=?)",
            Boolean.class,
            alert,
            session,
            access.user(who).getId()))) throw new NotFoundException("Alerte introuvable.");
    return mail.delivery(alert);
  }
}
