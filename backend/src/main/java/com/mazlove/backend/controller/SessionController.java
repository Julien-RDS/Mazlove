package com.mazlove.backend.controller;

import com.mazlove.backend.dto.CodeDTO;
import com.mazlove.backend.dto.SessionCreateDTO;
import com.mazlove.backend.dto.UserDTO;
import com.mazlove.backend.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Réservation et déroulé d'une séance. Tous les endpoints exigent un JWT.
 * Chaque action renvoie la séance à jour, pour que l'interface se rafraîchisse sans second appel.
 * On renvoie un DTO et jamais l'entité Session : ses relations LAZY feraient planter Jackson
 * et exposeraient le code d'arrivée au praticien.
 */
@RestController
@RequestMapping("/api/sessions")
@Tag(name = "Séances")
@SecurityRequirement(name = "bearerAuth")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Operation(summary = "Réserver une séance (le client est l'utilisateur connecté)")
    @PostMapping
    public ResponseEntity<UserDTO.SessionDTO> createSession(@Valid @RequestBody SessionCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.createSession(dto));
    }

    @Operation(summary = "Détail d'une séance (participants uniquement)")
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO.SessionDTO> get(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.getSession(id));
    }

    @Operation(summary = "Le praticien saisit le code d'arrivée du client : la séance démarre")
    @PostMapping("/{id}/validate-arrival")
    public ResponseEntity<UserDTO.SessionDTO> validateArrival(@PathVariable Long id, @Valid @RequestBody CodeDTO codeDTO) {
        return ResponseEntity.ok(sessionService.validateArrival(id, codeDTO.code()));
    }

    @Operation(summary = "Terminer une séance en cours")
    @PostMapping("/{id}/confirm-end")
    public ResponseEntity<UserDTO.SessionDTO> confirmEnd(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.confirmEnd(id));
    }

    @Operation(summary = "Annuler une séance qui n'a pas encore commencé")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<UserDTO.SessionDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.cancel(id));
    }
}
