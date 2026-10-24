package com.mazlove.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Avis sur une séance terminée. L'auteur est déduit du JWT, jamais du corps de la requête. */
public record ReviewCreateDTO(
    @NotNull(message = "La séance concernée est obligatoire.")
    Long sessionId,

    @Min(value = 1, message = "La note va de 1 à 5.")
    @Max(value = 5, message = "La note va de 1 à 5.")
    int rating,

    @Size(max = 1000, message = "Le commentaire est limité à 1000 caractères.")
    String comment
) {}
