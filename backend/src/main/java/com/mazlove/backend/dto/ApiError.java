package com.mazlove.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Format unique de toutes les erreurs renvoyées par l'API.
 * Le frontend affiche {@code message} ; {@code fields} détaille les champs invalides
 * d'un formulaire (clé = nom du champ, valeur = consigne à afficher sous le champ).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(int status, String message, Map<String, String> fields, Instant timestamp) {

    public static ApiError of(int status, String message) {
        return new ApiError(status, message, null, Instant.now());
    }

    public static ApiError of(int status, String message, Map<String, String> fields) {
        return new ApiError(status, message, fields, Instant.now());
    }
}
