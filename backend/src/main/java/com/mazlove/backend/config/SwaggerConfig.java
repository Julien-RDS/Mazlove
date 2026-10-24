package com.mazlove.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Documentation interactive : http://localhost:3001/swagger-ui.html
 * Le bouton « Authorize » accepte le JWT obtenu via /api/auth/verify-otp (sans le préfixe "Bearer").
 */
@OpenAPIDefinition(
    info = @Info(
        title = "Mazlove API",
        version = "1.0",
        description = "Réservation de séances de câlins professionnels : praticiens, séances, avis."
    )
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
@Configuration
public class SwaggerConfig {
    // Tout est déclaré par les annotations ci-dessus ; springdoc génère le reste à partir des contrôleurs.
}
