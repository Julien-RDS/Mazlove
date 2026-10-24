package com.mazlove.backend.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Mise à jour partielle du profil : un champ null n'est pas modifié.
 * Les contraintes ne s'appliquent qu'aux valeurs fournies (null est toujours accepté).
 */
public record UserUpdateDTO(
    @Size(min = 1, max = 100, message = "Le nom doit contenir entre 1 et 100 caractères.")
    String name,

    @Pattern(regexp = "^\\+?[0-9 .\\-]{6,19}$", message = "Numéro de téléphone invalide.")
    String phone
) {}
