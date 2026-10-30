package com.mazlove.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mazlove.backend.dto.ApiError;
import com.mazlove.backend.service.JWTService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JWTService jwtService;
  private final UserDetailsService userDetailsService;
  private final ObjectMapper objectMapper;

  /**
   * Origines autorisées à appeler l'API depuis un navigateur, via ALLOWED_ORIGINS. Ex :
   * ALLOWED_ORIGINS=https://app.mazlove.fr,https://admin.mazlove.fr En développement, le proxy Vite
   * rend les appels "même origine" : CORS n'intervient pas.
   */
  @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:3001}")
  private List<String> allowedOrigins;

  public SecurityConfig(
      JWTService jwtService, UserDetailsService userDetailsService, ObjectMapper objectMapper) {
    this.jwtService = jwtService;
    this.userDetailsService = userDetailsService;
    this.objectMapper = objectMapper;
  }

  /**
   * Règles d'accès. Ordre important : la première règle qui correspond l'emporte. Note Spring
   * Security 6 : un motif avec une double étoile au milieu du chemin (l'ancien motif « providers,
   * double étoile, reviews ») est interdit et fait planter le démarrage ; d'où les règles par
   * méthode HTTP ci-dessous.
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable) // API sans cookie de session : pas de CSRF possible
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth
                    // Pré-requêtes CORS des navigateurs : jamais authentifiées.
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/api/auth/**", "/api/public/trips/**", "/api/dev/login-as")
                    .permitAll()
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/providers/online")
                    .authenticated()
                    .requestMatchers("/actuator/health")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/uploads/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/providers/me/**")
                    .authenticated()
                    // Consultation libre des praticiens, de leurs créneaux et de leurs avis…
                    .requestMatchers(HttpMethod.GET, "/api/providers", "/api/providers/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/reviews/**")
                    .permitAll()
                    // …mais POST /api/providers et POST /api/providers/{id}/reviews exigent un
                    // compte.
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    // Poignée de main WebSocket : l'authentification se fait dans la trame STOMP
                    // CONNECT.
                    .requestMatchers("/ws/**")
                    .permitAll()
                    // Page d'erreur interne de Spring : sans cela, une 404 devient une 401.
                    .requestMatchers("/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                        (request, response, e) ->
                            writeError(
                                response,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "Connectez-vous pour continuer."))
                    .accessDeniedHandler(
                        (request, response, e) ->
                            writeError(
                                response,
                                HttpServletResponse.SC_FORBIDDEN,
                                "Vous n'avez pas accès à cette ressource.")))
        .addFilterBefore(
            new JWTAuthenticationFilter(jwtService, userDetailsService),
            UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /** Configuration CORS explicite (et non plus désactivée), limitée aux origines connues. */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Accept-Language"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
      throws Exception {
    return authConfig.getAuthenticationManager();
  }

  /** BCrypt (coût 10 par défaut, ~100 ms par vérification : lent exprès contre la force brute). */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Erreurs 401/403 au même format JSON que GlobalExceptionHandler (le front n'a qu'un cas à
   * gérer).
   */
  private void writeError(HttpServletResponse response, int status, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    objectMapper.writeValue(response.getOutputStream(), ApiError.of(status, message));
  }
}
