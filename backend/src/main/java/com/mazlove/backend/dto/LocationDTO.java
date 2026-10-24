package com.mazlove.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Coordonnées GPS en degrés décimaux (WGS84, comme navigator.geolocation).
 * Sert à la fois en entrée (création de fiche) et en sortie (position arrondie d'un praticien).
 */
public record LocationDTO(
    @NotNull(message = "La latitude est obligatoire.")
    @DecimalMin(value = "-90.0", message = "La latitude doit être comprise entre -90 et 90.")
    @DecimalMax(value = "90.0", message = "La latitude doit être comprise entre -90 et 90.")
    Double latitude,

    @NotNull(message = "La longitude est obligatoire.")
    @DecimalMin(value = "-180.0", message = "La longitude doit être comprise entre -180 et 180.")
    @DecimalMax(value = "180.0", message = "La longitude doit être comprise entre -180 et 180.")
    Double longitude
) {}
