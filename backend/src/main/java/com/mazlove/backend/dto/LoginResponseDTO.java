package com.mazlove.backend.dto;

/** Résultat de connexion : JWT immédiat en dev sans 2FA, sinon passage OTP requis. */
public record LoginResponseDTO(
    String message, String token, boolean mfaRequired, String challenge) {}
