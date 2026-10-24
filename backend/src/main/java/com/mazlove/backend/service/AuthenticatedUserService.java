package com.mazlove.backend.service;

import com.mazlove.backend.exception.UnauthorizedException;
import com.mazlove.backend.models.User;
import com.mazlove.backend.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Récupère l'utilisateur de la requête en cours à partir du contexte de sécurité,
 * rempli par JWTAuthenticationFilter (email = "subject" du JWT).
 *
 * Règle d'or : l'identité ne vient JAMAIS du corps de la requête (un client pourrait
 * y mettre n'importe quel id), toujours d'ici.
 */
@Component
public class AuthenticatedUserService {

    private final UserRepository userRepository;

    public AuthenticatedUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Email de l'utilisateur connecté, ou 401 si la requête est anonyme. */
    public String requireEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("Connectez-vous pour continuer.");
        }
        return auth.getName();
    }

    /** Entité User de l'utilisateur connecté (401 si le compte a été supprimé entre-temps). */
    public User requireUser() {
        return userRepository.findByEmail(requireEmail())
                .orElseThrow(() -> new UnauthorizedException("Votre session a expiré, reconnectez-vous."));
    }
}
