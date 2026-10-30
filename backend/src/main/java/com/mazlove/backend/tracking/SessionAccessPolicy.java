package com.mazlove.backend.tracking;

import com.mazlove.backend.exception.*;
import com.mazlove.backend.models.*;
import com.mazlove.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionAccessPolicy {
  private final SessionRepository sessions;
  private final UserRepository users;

  public User user(String email) {
    return users.findByEmail(email).orElseThrow(() -> new ForbiddenException("Compte inconnu."));
  }

  public Session assertParticipant(String email, Long id) {
    Session s =
        sessions.findById(id).orElseThrow(() -> new NotFoundException("Séance introuvable."));
    if (!s.getClient().getEmail().equals(email)
        && !s.getProvider().getUser().getEmail().equals(email)
        && user(email).getRole() != User.Role.ADMIN)
      throw new ForbiddenException("Cette séance ne vous concerne pas.");
    return s;
  }

  public Session lockProvider(String email, Long id) {
    sessions.lockById(id).orElseThrow(() -> new NotFoundException("Séance introuvable."));
    return assertProvider(email, id);
  }

  public Session assertProvider(String email, Long id) {
    Session s = assertParticipant(email, id);
    if (!s.getProvider().getUser().getEmail().equals(email))
      throw new ForbiddenException("Action réservée au prestataire.");
    return s;
  }
}
