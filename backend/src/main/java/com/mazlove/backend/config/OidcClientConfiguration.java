package com.mazlove.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/** Crée le client SSO uniquement lorsque issuer, client id et secret sont tous renseignés. */
@Configuration
@Conditional(OidcEnabledCondition.class)
public class OidcClientConfiguration {
  @Bean
  ClientRegistrationRepository oidcClientRegistrationRepository(
      OidcSettings settings,
      @org.springframework.beans.factory.annotation.Value(
              "${tracking.public-base-url:http://localhost:3000}")
          String publicBaseUrl) {
    return new BoundedOidcClients(settings, publicBaseUrl, java.time.Duration.ofSeconds(2));
  }
}
