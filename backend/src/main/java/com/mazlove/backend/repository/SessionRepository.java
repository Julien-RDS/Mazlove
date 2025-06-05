package com.mazlove.backend.repository;

import com.mazlove.backend.models.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    // Ajouter des méthodes de recherche personnalisées si nécessaire
}
