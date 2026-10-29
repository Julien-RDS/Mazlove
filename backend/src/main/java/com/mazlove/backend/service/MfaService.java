package com.mazlove.backend.service;

import com.mazlove.backend.dto.OtpDTO;
import com.mazlove.backend.util.Emails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;

/**
 * Double authentification par code à usage unique (OTP) à 6 chiffres.
 *
 * Les codes vivent dans Redis (clé "otp:{email}", expiration automatique après 5 min),
 * ce qui fonctionne avec plusieurs instances de l'API et survit à un redémarrage.
 * Un compteur d'essais (clé "otp-attempts:{email}") invalide le code après 5 erreurs :
 * sans lui, 1 million de combinaisons se testent en quelques minutes.
 */
@Service
public class MfaService {

    private static final Logger log = LoggerFactory.getLogger(MfaService.class);

    private static final String OTP_PREFIX = "otp:";
    private static final String ATTEMPTS_PREFIX = "otp-attempts:";
    private static final int OTP_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration OTP_TTL = Duration.ofMinutes(5);

    /** Une seule instance, thread-safe ; la recréer à chaque appel coûte cher en entropie. */
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final boolean devLogOtp;

    /**
     * @param devLogOtp MFA_DEV_LOG_OTP=true affiche aussi le code dans les logs en développement.
     */
    public MfaService(StringRedisTemplate redisTemplate,
                      EmailService emailService,
                      @Value("${mfa.dev-log-otp:false}") boolean devLogOtp) {
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
        this.devLogOtp = devLogOtp;
    }

    /**
     * Génère un nouveau code, le stocke (en écrasant l'éventuel précédent), remet le compteur
     * d'essais à zéro et l'« envoie ». C'est le SEUL point d'entrée pour créer un OTP : l'ancien
     * AuthController en générait un second dans sa réponse, ce qui rendait le premier invalide.
     */
    public void sendOtp(String email) {
        String normalized = Emails.normalize(email);
        String otp = generateOtp(normalized);
        redisTemplate.delete(ATTEMPTS_PREFIX + normalized);

        // Une erreur de livraison doit remonter au client : ne pas annoncer une connexion réussie
        // lorsque le code n'a pas été envoyé.
        emailService.sendOtpEmail(normalized, otp);

        if (devLogOtp) {
            log.warn("[DEV] Code de connexion pour {} : {} (MFA_DEV_LOG_OTP=true, à désactiver en production)",
                    normalized, otp);
        }
    }

    /**
     * Vérifie le code soumis. Usage unique : supprimé dès qu'il est validé.
     * Au-delà de MAX_ATTEMPTS essais, le code est détruit et il faut se reconnecter.
     */
    public boolean verifyOtp(OtpDTO otpDTO) {
        String email = Emails.normalize(otpDTO.email());
        String key = OTP_PREFIX + email;
        String attemptsKey = ATTEMPTS_PREFIX + email;

        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null) {
            return false;
        }

        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(attemptsKey, OTP_TTL);
        }
        if (attempts != null && attempts > MAX_ATTEMPTS) {
            redisTemplate.delete(key);
            redisTemplate.delete(attemptsKey);
            log.warn("Trop d'essais de code pour {} : code invalidé", email);
            return false;
        }

        // Comparaison à temps constant : ne révèle pas combien de chiffres sont corrects.
        boolean valid = MessageDigest.isEqual(
                stored.getBytes(StandardCharsets.UTF_8),
                otpDTO.code().getBytes(StandardCharsets.UTF_8));
        if (valid) {
            redisTemplate.delete(key);
            redisTemplate.delete(attemptsKey);
        }
        return valid;
    }

    /** Code numérique à 6 chiffres stocké avec expiration. Privé : passer par sendOtp(). */
    private String generateOtp(String normalizedEmail) {
        String otp = buildNumericCode(OTP_LENGTH);
        redisTemplate.opsForValue().set(OTP_PREFIX + normalizedEmail, otp, OTP_TTL);
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
