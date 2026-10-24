package com.mazlove.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

/**
 * Point central des caches Redis : noms des caches et invalidations.
 *
 * Les lectures sont mises en cache directement sur les services (@Cacheable sur
 * ProviderService#findNearby et ReviewService#findByProvider). Les invalidations passent
 * par ce bean distinct car un appel interne à une méthode @CacheEvict de la même classe
 * contournerait le proxy Spring et ne viderait rien.
 * Le RedisCacheManager est "transactionAware" : l'éviction n'a lieu qu'après le commit,
 * ce qui évite de remettre en cache une donnée pas encore enregistrée.
 */
@Service
public class CacheService {

    /** Listes de praticiens proches, clé = "lat:lng:page:taille". */
    public static final String PROVIDERS = "providers";

    /** Avis publics d'un praticien, clé = id du praticien. */
    public static final String REVIEWS = "reviews";

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    /**
     * Une nouvelle fiche ou un nouvel avis (qui change la note) peut modifier n'importe quelle
     * recherche géographique : on vide tout le cache des listes plutôt que de deviner les clés.
     */
    @CacheEvict(cacheNames = PROVIDERS, allEntries = true)
    public void evictNearbyProviders() {
        log.debug("Cache des listes de praticiens vidé");
    }

    @CacheEvict(cacheNames = REVIEWS, key = "#providerId")
    public void evictProviderReviews(Long providerId) {
        log.debug("Cache des avis du praticien {} vidé", providerId);
    }

    /** À appeler après une modification manuelle en base (script SQL, back-office…). */
    @CacheEvict(cacheNames = {PROVIDERS, REVIEWS}, allEntries = true)
    public void evictAll() {
        log.info("Tous les caches applicatifs ont été vidés");
    }
}
