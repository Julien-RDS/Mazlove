package com.mazlove.backend.tracking;

import static com.mazlove.backend.tracking.SessionStateMachine.*;

import com.mazlove.backend.dto.UserDTO;
import com.mazlove.backend.exception.*;
import com.mazlove.backend.models.*;
import com.mazlove.backend.service.SessionMapper;
import io.swagger.v3.oas.annotations.Operation;
import java.security.Principal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Transactional
public class TrackingRestController {
  private final SessionAccessPolicy access;
  private final SessionLifecycle lifecycle;
  private final SessionMapper mapper;
  private final ConsentService consent;

  public record Departure(TravelMode travelMode, Double lat, Double lng) {}

  @PutMapping("/api/sessions/{id}/meeting-point")
  @Operation(summary = "Compléter le point de rendez-vous avant le départ")
  public UserDTO.SessionDTO meeting(
      Principal p,
      @PathVariable Long id,
      @jakarta.validation.Valid @RequestBody MeetingPoint point) {
    Session s = access.lockParticipant(p.getName(), id);
    if (!s.getClient().getEmail().equals(p.getName()))
      throw new ForbiddenException("Le client choisit le rendez-vous.");
    if (s.getStatus() != Session.Status.PENDING && s.getStatus() != Session.Status.ACCEPTED)
      throw new IllegalStateException("Le rendez-vous ne peut plus être modifié après le départ.");
    s.setMeetingLatitude(point.lat());
    s.setMeetingLongitude(point.lng());
    s.setMeetingLabel(point.label());
    return mapper.toDTO(s, s.getClient());
  }

  @PostMapping("/api/sessions/{id}/accept")
  @Operation(summary = "Accepter une réservation")
  public UserDTO.SessionDTO accept(Principal p, @PathVariable Long id) {
    return move(p, id, Action.ACCEPT);
  }

  @PostMapping("/api/sessions/{id}/decline")
  @Operation(summary = "Refuser une réservation")
  public UserDTO.SessionDTO decline(Principal p, @PathVariable Long id) {
    return move(p, id, Action.DECLINE);
  }

  @PostMapping("/api/sessions/{id}/arrived")
  @Operation(summary = "Signaler son arrivée")
  public UserDTO.SessionDTO arrive(Principal p, @PathVariable Long id) {
    return move(p, id, Action.ARRIVE);
  }

  private UserDTO.SessionDTO move(Principal p, Long id, Action a) {
    Session s = access.lockProvider(p.getName(), id);
    lifecycle.move(s, a, Actor.PROVIDER);
    return mapper.toDTO(s, s.getProvider().getUser());
  }

  @PostMapping("/api/sessions/{id}/depart")
  @Operation(summary = "Partir vers le rendez-vous, avec consentement et position initiale")
  public UserDTO.SessionDTO depart(Principal p, @PathVariable Long id, @RequestBody Departure d) {
    Session s = access.lockProvider(p.getName(), id);
    consent.require(s.getProvider().getUser().getId());
    if (d.travelMode() == null
        || d.lat() == null
        || d.lng() == null
        || !Double.isFinite(d.lat())
        || !Double.isFinite(d.lng())
        || Math.abs(d.lat()) > 90
        || Math.abs(d.lng()) > 180)
      throw new BadRequestException("Choisissez un mode et activez votre position.");
    if (s.getMeetingLatitude() == null)
      throw new BadRequestException(
          "Cette ancienne réservation ne possède pas de point de rendez-vous.");
    s.setTravelMode(d.travelMode());
    lifecycle.move(s, Action.DEPART, Actor.PROVIDER);
    return mapper.toDTO(s, s.getProvider().getUser());
  }

  @GetMapping("/api/consents")
  @Operation(summary = "Consulter le consentement de localisation")
  public Map<String, Object> consent(Principal p) {
    return Map.of(
        "granted",
        consent.granted(access.user(p.getName()).getId()),
        "policyVersion",
        ConsentService.VERSION);
  }

  @PostMapping("/api/consents")
  @Operation(summary = "Consentir au partage de position pendant le trajet")
  public Map<String, Object> grant(Principal p) {
    consent.grant(access.user(p.getName()).getId());
    return consent(p);
  }
}
