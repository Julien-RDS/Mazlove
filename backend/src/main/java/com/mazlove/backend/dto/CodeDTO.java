package com.mazlove.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Code d'arrivée saisi par le praticien quand il retrouve le client (6 lettres/chiffres). */
public record CodeDTO(
    @NotBlank(message = "Saisissez le code donné par le client.")
    @Pattern(regexp = "^\\s*[A-Za-z0-9]{6}\\s*$", message = "Le code contient 6 lettres ou chiffres.")
    String code
) {}
