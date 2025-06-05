package com.mazlove.backend.controllers;

import com.mazlove.backend.models.Session;
import com.mazlove.backend.models.Provider;
import com.mazlove.backend.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @PostMapping
    public ResponseEntity<Session> createSession(@RequestParam Long providerId,
                                                @RequestParam String startTime,
                                                @RequestParam int duration) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        Session session = new Session();
        session.setProvider(provider);
        session.setStartTime(startTime);
        session.setDuration(duration);

        return ResponseEntity.ok(sessionRepository.save(session));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Session> getSession(@PathVariable Long id) {
        return sessionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
