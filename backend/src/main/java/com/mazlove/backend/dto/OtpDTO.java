package com.mazlove.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Seconde étape de connexion : le code à 6 chiffres reçu après LoginDTO. */
public record OtpDTO(
    @Email(message = "Adresse email invalide.")
    @NotBlank(message = "L'email est obligatoire.")
    String email,

    @NotBlank(message = "Saisissez le code reçu.")
    @Size(min = 6, max = 6, message = "Le code contient 6 chiffres.")
    @Pattern(regexp = "^[0-9]{6}$", message = "Le code contient 6 chiffres.")
    String code
) {}
