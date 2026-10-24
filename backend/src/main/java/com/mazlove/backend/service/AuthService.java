package com.mazlove.backend.service;

import com.mazlove.backend.dto.RegisterDTO;
import com.mazlove.backend.exception.ConflictException;
import com.mazlove.backend.exception.UnauthorizedException;
import com.mazlove.backend.models.User;
import com.mazlove.backend.repository.UserRepository;
import com.mazlove.backend.util.Emails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Inscription, vérification des identifiants et émission du JWT final. */
@Service
public class AuthService {

    /** Même message que l'email soit inconnu ou le mot de passe faux : on ne révèle pas qui est inscrit. */
    private static final String BAD_CREDENTIALS = "Email ou mot de passe incorrect.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;

    /**
     * Hash BCrypt factice comparé quand l'email n'existe pas, pour que la réponse prenne
     * le même temps dans les deux cas (sinon le chronométrage trahit les comptes existants).
     */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JWTService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("mazlove-timing-equalizer");
    }

    /**
     * Crée un compte CLIENT ou PROVIDER.
     * Lève une ConflictException (RuntimeException, HTTP 409) si l'email est déjà utilisé.
     */
    @Transactional
    public void register(RegisterDTO dto) {
        String email = Emails.normalize(dto.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Un compte existe déjà avec cet email. Connectez-vous ou utilisez une autre adresse.");
        }

        User user = new User();
        user.setEmail(email);
        user.setName(dto.name().trim());
        user.setPhone(dto.phone().trim());
        user.setPassword(passwordEncoder.encode(dto.password()));
        // RegisterDTO garantit déjà CLIENT|PROVIDER ; valueOf sert de seconde barrière.
        user.setRole(User.Role.valueOf(dto.role().trim().toUpperCase(Locale.ROOT)));
        userRepository.save(user);
    }

    /** Lève une UnauthorizedException (401) si le couple email/mot de passe est faux. */
    @Transactional(readOnly = true)
    public void validateCredentials(String email, String password) {
        User user = userRepository.findByEmail(Emails.normalize(email)).orElse(null);
        if (user == null) {
            passwordEncoder.matches(password, dummyHash);
            throw new UnauthorizedException(BAD_CREDENTIALS);
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthorizedException(BAD_CREDENTIALS);
        }
    }

    /** À n'appeler qu'après la vérification OTP réussie. */
    public String generateJwt(String email) {
        return jwtService.generateToken(Emails.normalize(email));
    }

    public boolean validateJwt(String token) {
        return jwtService.validateAndGetSubject(token).isPresent();
    }
}
