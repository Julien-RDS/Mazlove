package com.mazlove.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
    info = @Info(
        title = "Mazlove API",
        version = "1.0",
        description = "API documentation for Mazlove service"
    )
)
@Configuration
public class SwaggerConfig {
    // Configuration automatique via springdoc-openapi
}
