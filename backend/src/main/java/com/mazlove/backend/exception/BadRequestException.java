package com.mazlove.backend.exception;

/**
 * Requête valide syntaxiquement mais refusée par une règle métier
 * (créneau hors disponibilités, séance pas encore terminée…) : HTTP 400.
 * Le message est affiché tel quel à l'utilisateur, il doit donc dire quoi corriger.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
