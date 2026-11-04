package com.mazlove.backend.tracking;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class DevProfileGuard {
  public DevProfileGuard(Environment env) {
    boolean production = env.acceptsProfiles(Profiles.of("prod"));
    boolean development = env.acceptsProfiles(Profiles.of("dev & !prod"));
    boolean twoFactorEnabled = env.getProperty("auth.2fa-enabled", Boolean.class, true);
    if (production
        && (env.acceptsProfiles(Profiles.of("dev | demo"))
            || env.getProperty("mfa.dev-log-otp", Boolean.class, false)
            || env.getProperty("mazlove.dev.fixed-otp", Boolean.class, false)))
      throw new IllegalStateException(
          "Les profils dev/demo, les logs OTP et l'OTP fixe sont interdits en production.");
    if (!twoFactorEnabled && !development)
      throw new IllegalStateException(
          "La double authentification ne peut être désactivée qu'avec le profil dev.");
    if (production) {
      String secret = env.getProperty("jwt.secret", "");
      if (secret.length() < 32 || secret.contains("CHANGE_ME") || secret.startsWith("dev_secret"))
        throw new IllegalStateException("Une clé JWT privée est obligatoire en production.");
      String origin = env.getProperty("cors.allowed-origins", "");
      for (String value : origin.split(",", -1)) {
        java.net.URI uri = java.net.URI.create(value.trim());
        if (!"https".equals(uri.getScheme())
            || uri.getHost() == null
            || value.contains("*")
            || uri.getUserInfo() != null
            || uri.getQuery() != null
            || uri.getFragment() != null
            || !uri.getPath().isEmpty())
          throw new IllegalStateException(
              "Les origines de production doivent être des origines HTTPS explicites, sans"
                  + " chemin.");
      }
      java.net.URI publicUrl = java.net.URI.create(env.getProperty("tracking.public-base-url", ""));
      if (!"https".equals(publicUrl.getScheme()) || publicUrl.getHost() == null)
        throw new IllegalStateException("L’URL publique de production doit utiliser HTTPS.");
    }
  }
}
