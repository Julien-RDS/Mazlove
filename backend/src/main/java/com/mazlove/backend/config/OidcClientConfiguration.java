package com.mazlove.backend.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;

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
    return new ClientRegistrationRepository() {
      private volatile ClientRegistration registration;

      @Override
      public synchronized ClientRegistration findByRegistrationId(String id) {
        if (!"oidc".equals(id)) return null;
        if (registration == null)
          registration =
              ClientRegistrations.fromIssuerLocation(settings.issuer())
                  .registrationId("oidc")
                  .clientId(settings.clientId())
                  .clientSecret(settings.clientSecret())
                  .clientName(settings.providerName())
                  .redirectUri(publicBaseUrl.replaceAll("/+$", "") + "/login/oauth2/code/oidc")
                  .scope(List.of("openid", "profile", "email"))
                  .build();
        return registration;
      }
    };
  }
}
