package com.mazlove.backend.config;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.client.RestTemplate;

/** Une seule découverte bornée ; les autres demandes échouent immédiatement pendant celle-ci. */
final class BoundedOidcClients implements ClientRegistrationRepository {
  private final OidcSettings settings;
  private final String publicUrl;
  private final RestTemplate http;
  private final ReentrantLock loading = new ReentrantLock();
  private volatile ClientRegistration registration;
  private volatile long retryAfter;

  BoundedOidcClients(OidcSettings settings, String publicUrl, Duration timeout) {
    this.settings = settings;
    this.publicUrl = publicUrl;
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Math.toIntExact(timeout.toMillis()));
    factory.setReadTimeout(Math.toIntExact(timeout.toMillis()));
    http = new RestTemplate(factory);
  }

  @Override
  public ClientRegistration findByRegistrationId(String id) {
    if (!"oidc".equals(id)) return null;
    if (registration != null) return registration;
    if (System.nanoTime() < retryAfter || !loading.tryLock())
      throw new IllegalStateException("Fournisseur OIDC temporairement indisponible");
    try {
      if (registration != null) return registration;
      if (System.nanoTime() < retryAfter)
        throw new IllegalStateException("Découverte OIDC différée");
      Map<?, ?> metadata =
          http.getForObject(
              settings.issuer().replaceAll("/+$", "") + "/.well-known/openid-configuration",
              Map.class);
      if (metadata == null || !settings.issuer().equals(metadata.get("issuer")))
        throw new IllegalArgumentException("Issuer OIDC incohérent");
      var builder =
          ClientRegistration.withRegistrationId("oidc")
              .clientId(settings.clientId())
              .clientSecret(settings.clientSecret())
              .clientName(settings.providerName())
              .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
              .redirectUri(publicUrl.replaceAll("/+$", "") + "/login/oauth2/code/oidc")
              .scope("openid", "profile", "email")
              .issuerUri(settings.issuer())
              .authorizationUri(required(metadata, "authorization_endpoint"))
              .tokenUri(required(metadata, "token_endpoint"))
              .jwkSetUri(required(metadata, "jwks_uri"))
              .userNameAttributeName("sub");
      if (metadata.get("userinfo_endpoint") instanceof String uri && !uri.isBlank())
        builder.userInfoUri(uri);
      registration = builder.build();
      return registration;
    } catch (RuntimeException failure) {
      retryAfter = System.nanoTime() + Duration.ofSeconds(5).toNanos();
      throw failure;
    } finally {
      loading.unlock();
    }
  }

  private static String required(Map<?, ?> metadata, String key) {
    if (metadata.get(key) instanceof String value && !value.isBlank()) return value;
    throw new IllegalArgumentException("Métadonnée OIDC manquante : " + key);
  }
}
