package com.mazlove.backend.tracking;

import io.swagger.v3.oas.annotations.Operation;
import java.security.Principal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TrackingController {
  private final TrackingService service;

  @GetMapping("/api/tracking/{id}/snapshot")
  @Operation(summary = "État courant du trajet, réservé aux participants")
  public TrackingService.Snapshot snapshot(Principal p, @PathVariable long id) {
    return service.snapshot(p.getName(), id);
  }

  @MessageMapping("/tracking/{id}/position")
  public void position(Principal p, @DestinationVariable long id, PositionDTO dto) {
    service.ingest(p.getName(), id, dto);
  }

  @MessageExceptionHandler
  @SendToUser("/queue/errors")
  public Map<String, String> error(Exception e) {
    return Map.of(
        "message",
        e instanceof com.mazlove.backend.exception.ForbiddenException
            ? e.getMessage()
            : "Position refusée. Vérifiez votre connexion et votre séance.");
  }
}
