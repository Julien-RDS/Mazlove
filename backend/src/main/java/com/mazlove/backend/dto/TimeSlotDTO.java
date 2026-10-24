package com.mazlove.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Créneau hebdomadaire d'un praticien, ex. { "dayOfWeek": "MONDAY", "startTime": "09:00", "endTime": "18:00" }.
 * Le jour suit java.time.DayOfWeek pour être converti sans table de correspondance.
 */
public record TimeSlotDTO(
    @NotBlank(message = "Le jour est obligatoire.")
    @Pattern(regexp = "^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)$",
             message = "Jour invalide (attendu : MONDAY … SUNDAY).")
    String dayOfWeek,

    @NotBlank(message = "L'heure de début est obligatoire.")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Heure de début au format HH:mm.")
    String startTime,

    @NotBlank(message = "L'heure de fin est obligatoire.")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Heure de fin au format HH:mm.")
    String endTime
) {}
