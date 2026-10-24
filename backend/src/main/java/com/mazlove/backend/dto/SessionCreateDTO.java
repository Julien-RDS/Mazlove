package com.mazlove.backend.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Réservation d'une séance par le client connecté.
 * providerId est un Long : Jackson accepte aussi bien 12 que "12" dans le JSON.
 */
public record SessionCreateDTO(
    @NotNull(message = "Choisissez un praticien.")
    Long providerId,

    @NotNull(message = "Choisissez une date et une heure.")
    @Future(message = "La séance doit être programmée dans le futur.")
    LocalDateTime startTime,

    @Min(value = 1, message = "Une séance dure au moins 1 heure.")
    @Max(value = 12, message = "Une séance ne peut pas dépasser 12 heures.")
    int duration,

    @Size(max = 1000, message = "Les consignes sont limitées à 1000 caractères.")
    String notes
) {}
