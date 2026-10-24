package com.mazlove.backend.exception;

/** Ressource introuvable (praticien, séance, avis…) : traduit en HTTP 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
