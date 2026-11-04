package com.mazlove.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mazlove.backend.config.OidcClientConfiguration;
import com.mazlove.backend.config.OidcSettings;
import com.mazlove.backend.models.User;
import com.mazlove.backend.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class OidcAccountServiceTest {
  @Test
  void missingVerifiedClaimIsRejected() {
    var users = mock(UserRepository.class);
    var oidc = mock(OidcUser.class);
    when(oidc.getEmailVerified()).thenReturn(null);
    assertThrows(
        OAuth2AuthenticationException.class,
        () -> new OidcAccountService(users).requireExistingVerifiedEmail(oidc));
    verifyNoInteractions(users);
  }

  @Test
  void oidcIsDisabledWhenOneRequiredVariableIsMissing() {
    var environment =
        new MockEnvironment()
            .withProperty("auth.oidc.issuer", "https://accounts.example.test")
            .withProperty("auth.oidc.client-id", "client");

    assertFalse(new OidcSettings(environment).enabled());

    try (var context = new AnnotationConfigApplicationContext()) {
      context
          .getEnvironment()
          .getPropertySources()
          .addFirst(
              new org.springframework.core.env.MapPropertySource(
                  "oidc-test",
                  java.util.Map.of(
                      "auth.oidc.issuer", "https://accounts.example.test",
                      "auth.oidc.client-id", "client")));
      context.register(OidcSettings.class, OidcClientConfiguration.class);
      context.refresh();
      assertTrue(context.getBeansOfType(ClientRegistrationRepository.class).isEmpty());
    }
  }

  @Test
  void unverifiedEmailIsExplicitlyRejected() {
    UserRepository users = mock(UserRepository.class);
    OidcUser oidcUser = mock(OidcUser.class);
    when(oidcUser.getEmailVerified()).thenReturn(false);

    var error =
        assertThrows(
            OAuth2AuthenticationException.class,
            () -> new OidcAccountService(users).requireExistingVerifiedEmail(oidcUser));

    assertEquals("email_not_verified", error.getError().getErrorCode());
    verifyNoInteractions(users);
  }

  @Test
  void verifiedEmailLinksOnlyAnExistingAccount() {
    UserRepository users = mock(UserRepository.class);
    OidcUser oidcUser = mock(OidcUser.class);
    when(oidcUser.getEmailVerified()).thenReturn(true);
    when(oidcUser.getEmail()).thenReturn(" Alice@Example.com ");
    when(users.findByEmail("alice@example.com")).thenReturn(Optional.of(new User()));

    assertEquals(
        "alice@example.com", new OidcAccountService(users).requireExistingVerifiedEmail(oidcUser));
  }
}
