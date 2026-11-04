package com.mazlove.backend.service;

import com.mazlove.backend.dto.OtpDTO;
import com.mazlove.backend.util.Emails;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/**
 * Double authentification par code à usage unique (OTP) à 6 chiffres.
 *
 * <p>Les codes vivent dans Redis (clé "otp:{email}", expiration automatique après 5 min), ce qui
 * fonctionne avec plusieurs instances de l'API et survit à un redémarrage. Un compteur d'essais
 * (clé "otp-attempts:{email}") invalide le code après 5 erreurs : sans lui, 1 million de
 * combinaisons se testent en quelques minutes.
 */
@Service
public class MfaService {

  private static final Logger log = LoggerFactory.getLogger(MfaService.class);

  private static final String OTP_PREFIX = "otp:";
  private static final String ATTEMPTS_PREFIX = "otp-attempts:";
  private static final int OTP_LENGTH = 6;
  private static final DefaultRedisScript<Long> ISSUE =
      new DefaultRedisScript<>(
          """
          if tonumber(redis.call('GET', KEYS[2]) or '0') >= 5 then return 0 end
          if not redis.call('SET', KEYS[3], '1', 'NX', 'EX', 30) then return 0 end
          redis.call('SET', KEYS[1], ARGV[1], 'EX', 300)
          return 1
          """,
          Long.class);
  private static final DefaultRedisScript<Long> REMOVE =
      new DefaultRedisScript<>(
          """
          if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) end
          return 0
          """,
          Long.class);
  private static final DefaultRedisScript<Long> VERIFY =
      new DefaultRedisScript<>(
          """
          if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
          if tonumber(redis.call('GET', KEYS[2]) or '0') >= 5 then
            redis.call('DEL', KEYS[1]); return 0
          end
          if ARGV[2] == '1' then redis.call('DEL', KEYS[1], KEYS[2]); return 1 end
          local n = redis.call('INCR', KEYS[2])
          if n == 1 then redis.call('EXPIRE', KEYS[2], 300) end
          if n >= 5 then redis.call('DEL', KEYS[1]) end
          return 0
          """,
          Long.class);

  /** Une seule instance, thread-safe ; la recréer à chaque appel coûte cher en entropie. */
  private static final SecureRandom RANDOM = new SecureRandom();

  private final StringRedisTemplate redisTemplate;
  private final EmailService emailService;
  private final boolean devLogOtp;
  private final boolean enabled;
  private final org.springframework.core.env.Environment environment;

  /**
   * @param devLogOtp MFA_DEV_LOG_OTP=true affiche aussi le code dans les logs en développement.
   */
  public MfaService(
      StringRedisTemplate redisTemplate,
      EmailService emailService,
      @Value("${mfa.dev-log-otp:false}") boolean devLogOtp,
      @Value("${auth.2fa-enabled:true}") boolean enabled,
      org.springframework.core.env.Environment environment) {
    this.redisTemplate = redisTemplate;
    this.emailService = emailService;
    this.devLogOtp = devLogOtp;
    this.enabled = enabled;
    this.environment = environment;
  }

  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Génère un nouveau code sans remettre le budget d'essais à zéro. Un envoi au plus par 30 s.
   * C'est le SEUL point d'entrée pour créer un OTP : l'ancien AuthController en générait un second
   * dans sa réponse, ce qui rendait le premier invalide.
   */
  public String sendOtp(String email) {
    if (!enabled) {
      return null;
    }
    String normalized = Emails.normalize(email);
    String otp = generateOtp();
    byte[] random = new byte[32];
    RANDOM.nextBytes(random);
    String challenge = java.util.HexFormat.of().formatHex(random);
    String storedCode = challenge + ":" + otp;
    Long issued =
        redisTemplate.execute(
            ISSUE,
            List.of(
                OTP_PREFIX + normalized, ATTEMPTS_PREFIX + normalized, "otp-send:" + normalized),
            storedCode);
    if (!Long.valueOf(1).equals(issued))
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
          "Trop de demandes ou d'essais. Patientez avant de demander un nouveau code.");

    // Une erreur de livraison doit remonter au client : ne pas annoncer une connexion réussie
    // lorsque le code n'a pas été envoyé.
    try {
      emailService.sendOtpEmail(normalized, otp);
    } catch (org.springframework.mail.MailException failure) {
      try {
        redisTemplate.execute(REMOVE, List.of(OTP_PREFIX + normalized), storedCode);
      } catch (org.springframework.dao.DataAccessException cleanupFailure) {
        failure.addSuppressed(cleanupFailure);
      }
      throw failure;
    }

    if (devLogOtp) {
      log.warn(
          "[DEV] Code de connexion pour {} : {} (MFA_DEV_LOG_OTP=true, à désactiver en production)",
          normalized,
          otp);
    }
    return challenge;
  }

  /**
   * Vérifie le code soumis. Usage unique : supprimé dès qu'il est validé. Après cinq essais
   * erronés, le code est détruit et le budget reste bloqué jusqu'à son expiration.
   */
  public boolean verifyOtp(OtpDTO otpDTO) {
    if (otpDTO.challenge() == null || !otpDTO.challenge().matches("[0-9a-f]{64}")) return false;
    String email = Emails.normalize(otpDTO.email());
    String key = OTP_PREFIX + email;
    String attemptsKey = ATTEMPTS_PREFIX + email;

    String stored = redisTemplate.opsForValue().get(key);
    if (stored == null) {
      return false;
    }
    int separator = stored.indexOf(':');
    if (separator != 64
        || !MessageDigest.isEqual(
            stored.substring(0, separator).getBytes(StandardCharsets.UTF_8),
            otpDTO.challenge().getBytes(StandardCharsets.UTF_8))) return false;

    // Comparaison à temps constant : ne révèle pas combien de chiffres sont corrects.
    boolean valid =
        MessageDigest.isEqual(
            stored.getBytes(StandardCharsets.UTF_8),
            (otpDTO.challenge() + ":" + otpDTO.code()).getBytes(StandardCharsets.UTF_8));
    // Comparaison du code puis consommation atomique : deux requêtes concurrentes
    // ne peuvent pas réutiliser le même code ni réinitialiser le budget d'essais.
    return Long.valueOf(1)
        .equals(
            redisTemplate.execute(VERIFY, List.of(key, attemptsKey), stored, valid ? "1" : "0"));
  }

  /** Code numérique à 6 chiffres stocké avec expiration. Privé : passer par sendOtp(). */
  private String generateOtp() {
    String otp =
        environment.acceptsProfiles(org.springframework.core.env.Profiles.of("dev & !prod"))
                && environment.getProperty("mazlove.dev.fixed-otp", Boolean.class, false)
            ? "123456"
            : buildNumericCode(OTP_LENGTH);
    return otp;
  }

  private String buildNumericCode(int length) {
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(RANDOM.nextInt(10));
    }
    return sb.toString();
  }
}
