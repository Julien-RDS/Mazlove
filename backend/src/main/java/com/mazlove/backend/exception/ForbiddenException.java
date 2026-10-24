package com.mazlove.backend.exception;

/** Utilisateur connecté mais sans droit sur cette ressource (séance d'un autre…) : HTTP 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
