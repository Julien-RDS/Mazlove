package com.mazlove.backend.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mazlove.backend.models.User;
import com.mazlove.backend.repository.UserRepository;
import com.mazlove.backend.service.JWTService;
import com.mazlove.backend.service.MfaService;
import java.net.InetSocketAddress;
import java.security.KeyPairGenerator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.util.UriComponentsBuilder;

/** Aller-retour Spring Security réel, découverte HTTP locale et validation RSA du jeton OIDC. */
class OidcCallbackTest {
  @Test
  void verifiedProviderStillRequiresLocalOtp() throws Exception {
    callback(true);
  }

  @Test
  void unverifiedProviderNeverReceivesOtpOrJwt() throws Exception {
    callback(false);
  }

  void callback(boolean verified) throws Exception {
    var generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    var keys = generator.generateKeyPair();
    var nonce = new AtomicReference<String>();
    var mapper = new ObjectMapper();
    var server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    String issuer = "http://127.0.0.1:" + server.getAddress().getPort();
    var jwk =
        new com.nimbusds.jose.jwk.RSAKey.Builder(
                (java.security.interfaces.RSAPublicKey) keys.getPublic())
            .keyID("test")
            .build();
    server.createContext(
        "/",
        exchange -> {
          Object body;
          switch (exchange.getRequestURI().getPath()) {
            case "/.well-known/openid-configuration" ->
                body =
                    Map.of(
                        "issuer",
                        issuer,
                        "authorization_endpoint",
                        issuer + "/authorize",
                        "token_endpoint",
                        issuer + "/token",
                        "jwks_uri",
                        issuer + "/jwks",
                        "response_types_supported",
                        List.of("code"),
                        "subject_types_supported",
                        List.of("public"),
                        "id_token_signing_alg_values_supported",
                        List.of("RS256"));
            case "/jwks" -> body = Map.of("keys", List.of(jwk.toJSONObject()));
            case "/token" -> {
              String jwt =
                  io.jsonwebtoken.Jwts.builder()
                      .setIssuer(issuer)
                      .setSubject("provider-subject")
                      .setAudience("test-client")
                      .setIssuedAt(new Date())
                      .setExpiration(new Date(System.currentTimeMillis() + 60000))
                      .claim("email", "alice@example.test")
                      .claim("email_verified", verified)
                      .claim("nonce", nonce.get())
                      .setHeaderParam("kid", "test")
                      .signWith(keys.getPrivate(), io.jsonwebtoken.SignatureAlgorithm.RS256)
                      .compact();
              body =
                  Map.of(
                      "access_token",
                      "provider-access-token",
                      "token_type",
                      "Bearer",
                      "expires_in",
                      60,
                      "id_token",
                      jwt);
            }
            default -> {
              exchange.sendResponseHeaders(404, -1);
              exchange.close();
              return;
            }
          }
          var bytes = mapper.writeValueAsBytes(body);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
    try {
      new OidcSecurityTest()
          .runner
          .withUserConfiguration(OidcSettings.class, OidcClientConfiguration.class)
          .withPropertyValues(
              "auth.oidc.issuer=" + issuer,
              "auth.oidc.client-id=test-client",
              "auth.oidc.client-secret=test-secret")
          .run(
              context -> {
                assertNull(context.getStartupFailure());
                when(context.getBean(UserRepository.class).findByEmail("alice@example.test"))
                    .thenReturn(Optional.of(new User()));
                var mfa = context.getBean(MfaService.class);
                when(mfa.isEnabled()).thenReturn(true);
                var mvc =
                    MockMvcBuilders.webAppContextSetup(context)
                        .addFilter(
                            context.getBean(
                                "springSecurityFilterChain", jakarta.servlet.Filter.class))
                        .build();
                var begin = mvc.perform(get("/oauth2/authorization/oidc")).andReturn();
                assertEquals(302, begin.getResponse().getStatus());
                var params =
                    UriComponentsBuilder.fromUriString(begin.getResponse().getRedirectedUrl())
                        .build()
                        .getQueryParams();
                nonce.set(
                    java.net.URLDecoder.decode(
                        params.getFirst("nonce"), java.nio.charset.StandardCharsets.UTF_8));
                assertNotNull(nonce.get());
                var session = (MockHttpSession) begin.getRequest().getSession(false);
                var callback =
                    mvc.perform(
                            get("/login/oauth2/code/oidc")
                                .session(session)
                                .param(
                                    "state",
                                    java.net.URLDecoder.decode(
                                        params.getFirst("state"),
                                        java.nio.charset.StandardCharsets.UTF_8))
                                .param("code", "test-code"))
                        .andReturn();
                assertEquals(302, callback.getResponse().getStatus());
                assertTrue(session.isInvalid());
                String redirect = callback.getResponse().getRedirectedUrl();
                if (verified) {
                  assertTrue(redirect.contains("oidcEmail=alice%40example.test"), redirect);
                  verify(mfa).sendOtp("alice@example.test");
                } else {
                  assertTrue(redirect.contains("oidcError=email_not_verified"), redirect);
                  verify(mfa, never()).sendOtp(anyString());
                }
                verify(context.getBean(JWTService.class), never()).generateToken(anyString());
              });
    } finally {
      server.stop(0);
    }
  }
}
