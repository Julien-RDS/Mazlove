package com.mazlove.backend.tracking;

import com.mazlove.backend.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConsentService {
  public static final String VERSION = "2026-01";
  private final JdbcTemplate db;

  public boolean granted(long user) {
    return Boolean.TRUE.equals(
        db.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM consents WHERE user_id=? AND scope='LIVE_LOCATION_SHARING'"
                + " AND policy_version=? AND revoked_at IS NULL)",
            Boolean.class,
            user,
            VERSION));
  }

  public void require(long user) {
    if (!granted(user))
      throw new ForbiddenException("Acceptez le partage de position avant de partir.");
  }

  public void grant(long user) {
    if (!granted(user))
      db.update(
          "INSERT INTO consents(user_id,scope,policy_version) VALUES (?,'LIVE_LOCATION_SHARING',?)",
          user,
          VERSION);
  }
}
