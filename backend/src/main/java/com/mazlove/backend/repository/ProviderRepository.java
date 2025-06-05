package com.mazlove.backend.repository;

import com.mazlove.backend.models.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long> {
    // Ajouter des méthodes de recherche personnalisées si nécessaire
}
