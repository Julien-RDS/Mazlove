package com.mazlove.backend.tracking;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
public class TrackingPublisher {
  private final JdbcTemplate db;
  private final SimpMessagingTemplate ws;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void live(long id, Object payload) {
    var statuses =
        db.queryForList("SELECT status FROM sessions WHERE id=? FOR UPDATE", String.class, id);
    if (statuses.size() == 1 && statuses.get(0).equals("EN_ROUTE"))
      ws.convertAndSend("/topic/tracking." + id, payload);
  }
}
