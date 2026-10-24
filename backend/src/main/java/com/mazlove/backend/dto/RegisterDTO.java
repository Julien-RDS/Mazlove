package com.mazlove.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Création de compte.
 * Le rôle est limité à CLIENT ou PROVIDER : impossible de s'auto-attribuer ADMIN.
 */
public record RegisterDTO(
    @Email(message = "Adresse email invalide.")
    @NotBlank(message = "Saisissez votre email.")
    @Size(max = 255, message = "Email trop long.")
    String email,

    // BCrypt ignore tout ce qui dépasse 72 octets : on borne pour ne pas donner
    // l'illusion qu'un mot de passe de 100 caractères est entièrement pris en compte.
    @NotBlank(message = "Choisissez un mot de passe.")
    @Size(min = 8, max = 72, message = "Le mot de passe doit contenir entre 8 et 72 caractères.")
    String password,

    @NotBlank(message = "Indiquez votre prénom et votre nom.")
    @Size(max = 100, message = "Nom trop long (100 caractères maximum).")
    String name,

    // Chiffres, espaces, points, tirets et "+" initial : couvre 06 12 34 56 78 et +33 6 12 34 56 78.
    @NotBlank(message = "Indiquez votre numéro de téléphone.")
    @Pattern(regexp = "^\\+?[0-9 .\\-]{6,19}$", message = "Numéro de téléphone invalide.")
    String phone,

    @NotNull(message = "Indiquez si vous cherchez ou proposez des séances.")
    @Pattern(regexp = "^(CLIENT|PROVIDER)$", message = "Le rôle doit être CLIENT ou PROVIDER.")
    String role
) {}
