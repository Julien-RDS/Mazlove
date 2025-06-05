package com.mazlove.backend.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CacheService {
    
    @Cacheable(value = "providers", key = "#lat + '-' + #lng")
    public List<ProviderDTO> getNearbyProviders(double lat, double lng) {
        // Cette méthode sera mise en cache avec une clé basée sur les coordonnées
        return providerService.findNearby(lat, lng);
    }

    @Cacheable(value = "reviews", key = "#providerId")
    public List<ReviewDTO> getProviderReviews(String providerId) {
        // Cette méthode sera mise en cache avec la clé providerId
        return reviewService.findByProvider(providerId);
    }

    @CacheEvict(value = {"providers", "reviews"}, allEntries = true)
    public void evictAllCaches() {
        // Évict toutes les entrées du cache
    }

    @CacheEvict(value = "providers", key = "#lat + '-' + #lng")
    public void evictProviderCache(double lat, double lng) {
        // Évict le cache des providers pour ces coordonnées
    }

    @CacheEvict(value = "reviews", key = "#providerId")
    public void evictReviewsCache(String providerId) {
        // Évict le cache des reviews pour ce provider
    }
}
