package com.mazlove.backend.config;

import com.mazlove.backend.service.JWTService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lit l'en-tête "Authorization: Bearer <jwt>" et, si le jeton est valide, déclare
 * l'utilisateur comme authentifié pour la durée de la requête.
 *
 * Volontairement PAS annoté @Component : Spring Boot enregistrerait alors le filtre une
 * seconde fois dans la chaîne servlet globale. Il est instancié dans SecurityConfig.
 *
 * Un jeton absent, expiré ou falsifié ne bloque rien ici : la requête continue en anonyme.
 * Les endpoints publics (liste des praticiens) restent donc accessibles même avec un vieux
 * jeton en mémoire côté navigateur, et les endpoints protégés renvoient un 401 propre.
 */
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JWTAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JWTService jwtService;
    private final UserDetailsService userDetailsService;

    public JWTAuthenticationFilter(JWTService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String jwt = authHeader.substring(BEARER_PREFIX.length()).trim();
            jwtService.validateAndGetSubject(jwt).ifPresent(email -> authenticate(email, request));
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String email, HttpServletRequest request) {
        try {
            // Recharger le compte à chaque requête : un compte supprimé perd l'accès
            // immédiatement, même si son jeton n'a pas encore expiré.
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch (UsernameNotFoundException e) {
            log.debug("JWT valide mais compte introuvable : {}", email);
        }
    }
}
