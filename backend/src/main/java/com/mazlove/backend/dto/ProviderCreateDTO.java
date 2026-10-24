package com.mazlove.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Création de la fiche praticien du compte connecté.
 * Aligné sur le modèle Provider : "bio" (et non "description"), pas de nom
 * (on reprend celui du compte) ni de "verified" (réservé aux admins).
 */
public record ProviderCreateDTO(
    @NotBlank(message = "Présentez-vous en quelques lignes.")
    @Size(max = 500, message = "La présentation est limitée à 500 caractères.")
    String bio,

    @NotBlank(message = "Choisissez un type de séance.")
    @Size(max = 100, message = "Le type de séance est limité à 100 caractères.")
    String category,

    @NotNull(message = "Indiquez votre zone d'intervention.")
    @Valid
    LocationDTO location,

    @NotEmpty(message = "Ajoutez au moins un créneau de disponibilité.")
    List<@Valid TimeSlotDTO> availability,

    // Uniquement des URL https : évite les "javascript:" ou contenus mixtes dans les <img>.
    @Size(max = 10, message = "10 photos maximum.")
    List<@Pattern(regexp = "^https://\\S{1,2040}$", message = "Chaque photo doit être une URL https.") String> images
) {}
