package com.mazlove.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mazlove.backend.repository.UserRepository;
import com.mazlove.backend.service.AuthService;
import com.mazlove.backend.service.JWTService;
import com.mazlove.backend.service.MfaService;
import com.mazlove.backend.service.OidcAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OidcSecurityTest {
  @org.springframework.context.annotation.Configuration
  @org.springframework.web.servlet.config.annotation.EnableWebMvc
  static class MvcConfiguration {}

  private final WebApplicationContextRunner runner =
      new WebApplicationContextRunner()
          .withUserConfiguration(
              MvcConfiguration.class,
              SecurityConfig.class,
              AuthService.class,
              OidcLoginHandlers.class,
              OidcAccountService.class)
          .withBean(ObjectMapper.class, () -> new ObjectMapper().findAndRegisterModules())
          .withBean(JWTService.class, () -> mock(JWTService.class))
          .withBean(UserRepository.class, () -> mock(UserRepository.class))
          .withBean(UserDetailsService.class, () -> mock(UserDetailsService.class))
          .withBean(MfaService.class, () -> mock(MfaService.class));

  @Test
  void securityAndRealAuthServiceStartWithoutCircularDependency() {
    runner.run(context -> assertThat(context).hasNotFailed());
  }

  @Test
  void oidcStartsButSessionAuthenticationCannotBypassLocalOtp() {
    runner
        .withBean(
            ClientRegistrationRepository.class,
            () ->
                new InMemoryClientRegistrationRepository(
                    ClientRegistration.withRegistrationId("oidc")
                        .clientId("test-client")
                        .clientSecret("test-secret")
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .redirectUri("http://localhost/login/oauth2/code/oidc")
                        .scope("openid", "email")
                        .authorizationUri("https://identity.example/authorize")
                        .tokenUri("https://identity.example/token")
                        .jwkSetUri("https://identity.example/jwks")
                        .userNameAttributeName("sub")
                        .build()))
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              var mvc =
                  MockMvcBuilders.webAppContextSetup(context)
                      .addFilter(
                          context.getBean(
                              "springSecurityFilterChain", jakarta.servlet.Filter.class))
                      .build();
              var session = new MockHttpSession();
              session.setAttribute(
                  "SPRING_SECURITY_CONTEXT",
                  new SecurityContextImpl(
                      new UsernamePasswordAuthenticationToken(
                          "sso-user", "", java.util.List.of())));
              mvc.perform(get("/api/users/me").session(session))
                  .andExpect(status().isUnauthorized());
              mvc.perform(get("/oauth2/authorization/oidc")).andExpect(status().is3xxRedirection());
            });
  }
}
