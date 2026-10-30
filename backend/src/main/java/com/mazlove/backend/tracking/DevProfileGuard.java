package com.mazlove.backend.tracking;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class DevProfileGuard {
  public DevProfileGuard(Environment env) {
    if (env.acceptsProfiles(Profiles.of("prod"))
        && (env.acceptsProfiles(Profiles.of("dev"))
            || env.getProperty("mazlove.dev.fixed-otp", Boolean.class, false)))
      throw new IllegalStateException("Le profil dev et l'OTP fixe sont interdits en production.");
  }
}
