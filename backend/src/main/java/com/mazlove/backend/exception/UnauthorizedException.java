package com.mazlove.backend.exception;

/** Identifiants, code OTP ou session de connexion invalides : HTTP 401. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
