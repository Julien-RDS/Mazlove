package com.mazlove.backend.dto;

/** JWT renvoyé après validation de l'OTP, à envoyer ensuite dans l'en-tête "Authorization: Bearer …". */
public record TokenDTO(String token) {}
