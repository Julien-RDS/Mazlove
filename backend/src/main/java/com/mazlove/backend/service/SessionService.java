package com.mazlove.backend.service;

import com.mazlove.backend.dto.SessionCreateDTO;
import com.mazlove.backend.models.Session;
import com.mazlove.backend.models.User;
import com.mazlove.backend.repository.SessionRepository;
import com.mazlove.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SessionService {
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    @Autowired
    public SessionService(SessionRepository sessionRepository, UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Session createSession(SessionCreateDTO dto) {
        User client = userRepository.findById(UUID.fromString(dto.providerId()))
            .orElseThrow(() -> new RuntimeException("Provider not found"));

        Session session = new Session();
        session.setClient(client);
        session.setProviderId(UUID.fromString(dto.providerId()));
        session.setDatetime(dto.startTime());
        session.setStatus(Session.Status.PENDING);
        session.setCodeValidation(generateValidationCode());
        session.setNotes(null);

        return sessionRepository.save(session);
    }

    @Transactional
    public void validateArrival(Long sessionId, String code) {
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getCodeValidation().equals(code)) {
            throw new RuntimeException("Invalid validation code");
        }

        session.setStatus(Session.Status.ACCEPTED);
        sessionRepository.save(session);
    }

    @Transactional
    public void confirmEnd(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setStatus(Session.Status.DONE);
        sessionRepository.save(session);
    }

    private String generateValidationCode() {
        return UUID.randomUUID().toString().substring(0, 6);
    }
}
