package com.mazlove.backend.config;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Lecture centralisée de l'activation OIDC : les trois paramètres sont obligatoires. */
@Component
public class OidcSettings {
  private final String issuer;
  private final String clientId;
  private final String clientSecret;

  public OidcSettings(Environment environment) {
    issuer = environment.getProperty("auth.oidc.issuer", "").trim();
    clientId = environment.getProperty("auth.oidc.client-id", "").trim();
    clientSecret = environment.getProperty("auth.oidc.client-secret", "").trim();
  }

  public boolean enabled() {
    return !issuer.isBlank() && !clientId.isBlank() && !clientSecret.isBlank();
  }

  public String issuer() {
    return issuer;
  }

  public String clientId() {
    return clientId;
  }

  public String clientSecret() {
    return clientSecret;
  }

  public String providerName() {
    String lower = issuer.toLowerCase(java.util.Locale.ROOT);
    if (lower.contains("accounts.google.com")) return "Google";
    if (lower.contains("microsoftonline.com")) return "Microsoft";
    return "SSO";
  }
}
