package com.mazlove.backend.controller;

import com.mazlove.backend.dto.SessionCreateDTO;
import com.mazlove.backend.dto.CodeDTO;
import com.mazlove.backend.models.Session;
import com.mazlove.backend.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {
    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<Session> createSession(@Valid @RequestBody SessionCreateDTO dto) {
        return ResponseEntity.ok(sessionService.createSession(dto));
    }

    @PostMapping("/{id}/validate-arrival")
    public ResponseEntity<?> validateArrival(@PathVariable Long id, @Valid @RequestBody CodeDTO codeDTO) {
        sessionService.validateArrival(id, codeDTO.code());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/confirm-end")
    public ResponseEntity<?> confirmEnd(@PathVariable Long id) {
        sessionService.confirmEnd(id);
        return ResponseEntity.ok().build();
    }
}
