package com.mazlove.backend.exception;

/** L'action entre en conflit avec l'état existant (email déjà pris, avis déjà laissé…) : HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
