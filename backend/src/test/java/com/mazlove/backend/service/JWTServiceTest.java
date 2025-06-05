package com.mazlove.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class JWTServiceTest {
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private long expiration;
    
    private JWTService jwtService;

    @BeforeEach
    public void setUp() {
        jwtService = new JWTService(secret, expiration);
    }

    @Test
    public void testGenerateToken() {
        String token = jwtService.generateToken("test@example.com");
        assertNotNull(token);
        assertTrue(token.startsWith("eyJ"));
    }

    @Test
    public void testExtractEmail() {
        String token = jwtService.generateToken("test@example.com");
        String email = jwtService.extractEmail(token);
        assertEquals("test@example.com", email);
    }

    @Test
    public void testTokenExpiration() {
        String token = jwtService.generateToken("test@example.com");
        assertTrue(jwtService.isTokenValid(token, "test@example.com"));
    }

    @Test
    public void testInvalidToken() {
        String invalidToken = "invalid.token.string";
        assertFalse(jwtService.isTokenValid(invalidToken, "test@example.com"));
    }

    @Test
    public void testTokenWithWrongEmail() {
        String token = jwtService.generateToken("test@example.com");
        assertFalse(jwtService.isTokenValid(token, "wrong@example.com"));
    }
}
