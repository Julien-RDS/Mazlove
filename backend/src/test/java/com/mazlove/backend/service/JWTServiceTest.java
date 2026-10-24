package com.mazlove.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unitaires purs : pas de contexte Spring, donc ni PostgreSQL ni Redis nécessaires.
 * Se lancent avec "mvn test" en quelques millisecondes.
 */
class JWTServiceTest {

    /** 47 caractères : au-dessus du minimum de 32 octets exigé pour HS256. */
    private static final String SECRET = "une-cle-de-test-suffisamment-longue-pour-hs256";
    private static final String EMAIL = "test@example.com";

    private JWTService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JWTService(SECRET, 60_000);
    }

    @Test
    void generatedTokenIsAJwt() {
        String token = jwtService.generateToken(EMAIL);
        assertNotNull(token);
        // Un JWT commence par l'en-tête {"alg":... encodé en base64url, soit "eyJ".
        assertTrue(token.startsWith("eyJ"));
    }

    @Test
    void extractEmailReturnsTheSubject() {
        String token = jwtService.generateToken(EMAIL);
        assertEquals(EMAIL, jwtService.extractEmail(token));
    }

    @Test
    void tokenIsValidForItsOwnEmail() {
        String token = jwtService.generateToken(EMAIL);
        assertTrue(jwtService.isTokenValid(token, EMAIL));
    }

    @Test
    void malformedTokenIsRejectedWithoutThrowing() {
        // Le filtre JWT s'appuie sur ce comportement : un jeton pourri = requête anonyme, pas une 500.
        assertFalse(jwtService.isTokenValid("invalid.token.string", EMAIL));
        assertTrue(jwtService.validateAndGetSubject("invalid.token.string").isEmpty());
    }

    @Test
    void tokenIssuedForAnotherEmailIsRejected() {
        String token = jwtService.generateToken(EMAIL);
        assertFalse(jwtService.isTokenValid(token, "wrong@example.com"));
    }

    @Test
    void expiredTokenIsRejected() {
        // Expiration négative : le jeton est déjà périmé au moment où il est émis.
        String expired = new JWTService(SECRET, -1_000).generateToken(EMAIL);
        assertFalse(jwtService.isTokenValid(expired, EMAIL));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String forged = new JWTService("une-autre-cle-tout-aussi-longue-mais-differente", 60_000)
                .generateToken(EMAIL);
        assertFalse(jwtService.isTokenValid(forged, EMAIL));
    }

    @Test
    void tooShortSecretPreventsStartup() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new JWTService("trop-courte", 60_000));
        assertTrue(error.getMessage().contains("JWT_SECRET"));
    }
}
