package com.mazlove.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Avis public sur un praticien.
 * Mis en cache Redis comme ProviderDTO, d'où la classe mutable avec constructeur vide.
 */
@Data
public class ReviewDTO {
    private Long id;
    private Long userId;

    /** Nom affiché de l'auteur, abrégé en « Prénom N. » pour préserver sa vie privée. */
    private String authorName;

    private Long sessionId;
    private Long providerId;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
}
