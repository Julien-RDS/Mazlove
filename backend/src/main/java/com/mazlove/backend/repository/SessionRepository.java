package com.mazlove.backend.repository;

import com.mazlove.backend.models.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    /**
     * Séances actives d'un praticien commençant dans une fenêtre donnée.
     * SessionService s'en sert pour refuser une réservation qui chevaucherait une autre :
     * la fenêtre est élargie de 12 h (durée max) en amont pour attraper une séance
     * commencée plus tôt et encore en cours.
     */
    List<Session> findByProvider_IdAndStatusInAndDatetimeBetween(Long providerId,
                                                                Collection<Session.Status> statuses,
                                                                LocalDateTime from,
                                                                LocalDateTime to);
}
