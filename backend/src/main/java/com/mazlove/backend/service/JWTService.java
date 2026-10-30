package com.mazlove.backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Émission et vérification des JWT (API jjwt 0.11.x, signature HMAC-SHA256). Le "subject" du jeton
 * est l'email normalisé de l'utilisateur.
 */
@Service
public class JWTService {

  private static final Logger log = LoggerFactory.getLogger(JWTService.class);

  /** HS256 exige une clé d'au moins 256 bits, soit 32 octets. */
  private static final int MIN_SECRET_BYTES = 32;

  /** Valeur par défaut d'application.properties : acceptable en local, jamais en production. */
  private static final String PLACEHOLDER_SECRET = "CHANGE_ME_IN_PRODUCTION_USE_ENV_VAR";

  private final Key signingKey;
  private final long expirationMs;

  /**
   * Valide la clé au démarrage : avec une clé trop courte, l'application refuse de démarrer avec un
   * message explicite, au lieu d'échouer à la première connexion.
   */
  public JWTService(
      @Value("${jwt.secret}") String secret, @Value("${jwt.expiration}") long expirationMs) {
    byte[] secretBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
    if (secretBytes.length < MIN_SECRET_BYTES) {
      throw new IllegalStateException(
          "JWT_SECRET doit faire au moins "
              + MIN_SECRET_BYTES
              + " octets (256 bits) pour HS256 ; "
              + "longueur actuelle : "
              + secretBytes.length
              + ". Générez-en une avec : openssl rand -base64 48");
    }
    if (PLACEHOLDER_SECRET.equals(secret)) {
      log.warn(
          "JWT_SECRET n'est pas défini : la clé par défaut est utilisée. N'importe qui connaissant"
              + " le code source peut forger des jetons. À ne jamais déployer ainsi.");
    }
    this.signingKey = Keys.hmacShaKeyFor(secretBytes);
    this.expirationMs = expirationMs;
  }

  public String generateToken(String email) {
    long now = System.currentTimeMillis();
    return Jwts.builder()
        .setSubject(email)
        .setIssuedAt(new Date(now))
        .setExpiration(new Date(now + expirationMs))
        .signWith(signingKey, SignatureAlgorithm.HS256)
        .compact();
  }

  /**
   * Vérifie signature et expiration en une seule lecture, et renvoie l'email si tout est bon. Ne
   * lève jamais d'exception : un jeton expiré ou falsifié donne simplement Optional.empty(), la
   * requête continue alors en anonyme (et reçoit un 401 si l'endpoint est protégé).
   */
  public Optional<String> validateAndGetSubject(String token) {
    try {
      return Optional.ofNullable(extractAllClaims(token).getSubject());
    } catch (JwtException | IllegalArgumentException e) {
      log.debug("JWT rejeté : {}", e.getClass().getSimpleName());
      return Optional.empty();
    }
  }

  /** Lève une JwtException si le jeton est invalide ou expiré. */
  public long expiresAt(String token) {
    return extractAllClaims(token).getExpiration().getTime();
  }

  public String extractEmail(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    return claimsResolver.apply(extractAllClaims(token));
  }

  /** true si le jeton est signé par nous, non expiré et émis pour cet email. */
  public boolean isTokenValid(String token, String email) {
    return validateAndGetSubject(token).map(subject -> subject.equals(email)).orElse(false);
  }

  /** parseClaimsJws vérifie la signature ET la date d'expiration (ExpiredJwtException sinon). */
  private Claims extractAllClaims(String token) {
    return Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token).getBody();
  }
}
