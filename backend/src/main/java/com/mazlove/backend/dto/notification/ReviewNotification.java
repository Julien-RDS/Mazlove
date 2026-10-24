package com.mazlove.backend.dto.notification;

/**
 * Nouvel avis publié sur un praticien, diffusé sur /topic/providers/{providerId}/reviews.
 * Ne contient que des informations déjà publiques.
 */
public record ReviewNotification(Long providerId, int rating, String message) {}
