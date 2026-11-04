package com.mazlove.backend.dto;

/** Configuration publique du formulaire de connexion, sans identifiant client ni secret. */
public record AuthConfigDTO(boolean oidcEnabled, String oidcProvider, boolean twoFactorEnabled) {}
