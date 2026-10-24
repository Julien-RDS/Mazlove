package com.mazlove.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Première étape de connexion : identifiants, avant l'envoi du code OTP. */
public record LoginDTO(
    @Email(message = "Adresse email invalide.")
    @NotBlank(message = "Saisissez votre email.")
    String email,

    @NotBlank(message = "Saisissez votre mot de passe.")
    String password
) {}
