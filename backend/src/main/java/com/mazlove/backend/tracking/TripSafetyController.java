package com.mazlove.backend.tracking;

import com.mazlove.backend.exception.*;
import com.mazlove.backend.models.User;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Transactional
public class TripSafetyController {
  private final TripSafetyService safety;
  private final SessionAccessPolicy access;
  private final JdbcTemplate db;
  private final TrackingStore store;

  @PostMapping("/api/sessions/{id}/share")
  @Operation(summary = "Créer un lien de suivi temporaire")
  public Object share(Principal p, @PathVariable long id) {
    return safety.share(p.getName(), id);
  }

  @DeleteMapping("/api/sessions/{id}/share")
  @Operation(summary = "Révoquer ses liens de suivi")
  public void revoke(Principal p, @PathVariable long id) {
    safety.revoke(p.getName(), id);
  }

  @GetMapping("/api/public/trips/{token}")
  @Operation(summary = "Vue publique minimale du trajet")
  public Object publicTrip(@PathVariable String token, HttpServletRequest r) {
    return safety.publicTrip(token, r.getRemoteAddr());
  }

  @PostMapping("/api/sessions/{id}/sos")
  @Operation(summary = "Alerter ses proches et l'équipe, sans appeler les secours")
  public Object sos(Principal p, @PathVariable long id) {
    return safety.sos(p.getName(), id);
  }

  public record Contact(
      @NotBlank @Size(max = 100) String name,
      @Email @Size(max = 255) String email,
      @Size(max = 30) String phone) {}

  @GetMapping("/api/users/me/trusted-contacts")
  @Operation(summary = "Lister ses contacts de confiance")
  public Object contacts(Principal p) {
    return db.queryForList(
        "SELECT id,name,email,phone FROM trusted_contacts WHERE user_id=?",
        access.user(p.getName()).getId());
  }

  @PostMapping("/api/users/me/trusted-contacts")
  @Operation(summary = "Ajouter un contact de confiance")
  public void add(Principal p, @Valid @RequestBody Contact c) {
    if ((c.email() == null || c.email().isBlank()) && (c.phone() == null || c.phone().isBlank()))
      throw new BadRequestException("Renseignez un email ou un téléphone.");
    db.update(
        "INSERT INTO trusted_contacts(user_id,name,email,phone) VALUES (?,?,?,?)",
        access.user(p.getName()).getId(),
        c.name(),
        c.email(),
        c.phone());
  }

  @DeleteMapping("/api/users/me/trusted-contacts/{id}")
  @Operation(summary = "Supprimer un contact de confiance")
  public void delete(Principal p, @PathVariable long id) {
    db.update(
        "DELETE FROM trusted_contacts WHERE id=? AND user_id=?",
        id,
        access.user(p.getName()).getId());
  }

  private void admin(Principal p) {
    if (access.user(p.getName()).getRole() != User.Role.ADMIN)
      throw new ForbiddenException("Accès réservé à l'équipe.");
  }

  @GetMapping("/api/admin/sos")
  @Operation(summary = "Alertes SOS ouvertes, équipe uniquement")
  public Object alerts(Principal p) {
    admin(p);
    return db.queryForList(
        "SELECT a.*,u.name FROM sos_alerts a JOIN users u ON u.id=a.raised_by WHERE a.status='OPEN'"
            + " ORDER BY created_at DESC");
  }

  @PatchMapping("/api/admin/sos/{id}/resolve")
  @Operation(summary = "Résoudre une alerte")
  public void resolve(Principal p, @PathVariable long id) {
    admin(p);
    db.update(
        "UPDATE sos_alerts SET status='RESOLVED',resolved_at=CURRENT_TIMESTAMP WHERE id=?", id);
  }

  @DeleteMapping("/api/users/me")
  @Operation(summary = "Supprimer son compte et ses données de suivi")
  public void account(Principal p) {
    long id = access.user(p.getName()).getId();
    for (Long sid :
        db.queryForList(
            "SELECT s.id FROM sessions s JOIN providers p ON p.id=s.provider_id WHERE s.client_id=?"
                + " OR p.user_id=?",
            Long.class,
            id,
            id)) store.clear(sid);
    db.update("DELETE FROM users WHERE id=?", id);
  }
}
