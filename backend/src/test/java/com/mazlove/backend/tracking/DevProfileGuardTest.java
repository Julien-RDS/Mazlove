package com.mazlove.backend.tracking;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.env.MockEnvironment;

class DevProfileGuardTest {
  @Test
  void productionRejectsOtpLoggingAndDemoAtContextStartup() {
    for (String config : java.util.List.of("logs", "demo")) {
      try (var context = new AnnotationConfigApplicationContext()) {
        context
            .getEnvironment()
            .setActiveProfiles(
                config.equals("demo") ? new String[] {"prod", "demo"} : new String[] {"prod"});
        context
            .getEnvironment()
            .getPropertySources()
            .addFirst(
                new org.springframework.core.env.MapPropertySource(
                    "test",
                    java.util.Map.of(
                        "mfa.dev-log-otp", config.equals("logs") ? "true" : "false",
                        "jwt.secret", "a-private-production-key-at-least-thirty-two-bytes",
                        "cors.allowed-origins", "https://mazlove.example",
                        "tracking.public-base-url", "https://mazlove.example")));
        context.register(DevProfileGuard.class);
        assertThrows(org.springframework.beans.BeansException.class, context::refresh);
      }
    }
  }

  @Test
  void twoFactorCannotBeDisabledInTestOrDefaultProfile() {
    for (String profile : java.util.List.of("test", "default")) {
      var env = new MockEnvironment().withProperty("auth.2fa-enabled", "false");
      env.setActiveProfiles(profile);
      assertThrows(IllegalStateException.class, () -> new DevProfileGuard(env));
    }
  }

  @Test
  void mixedProfilesFailClosed() {
    var env = new MockEnvironment();
    env.setActiveProfiles("dev", "prod");
    assertThrows(IllegalStateException.class, () -> new DevProfileGuard(env));
  }

  @Test
  void productionHasNoDevController() {
    try (var c = new AnnotationConfigApplicationContext()) {
      c.getEnvironment().setActiveProfiles("prod");
      c.register(DevSimulatorController.class, SimulatorService.class);
      c.refresh();
      assertTrue(c.getBeansOfType(DevSimulatorController.class).isEmpty());
      assertTrue(c.getBeansOfType(SimulatorService.class).isEmpty());
    }
  }

  @Test
  void fixedOtpIsForbiddenInProd() {
    var env = new MockEnvironment().withProperty("mazlove.dev.fixed-otp", "true");
    env.setActiveProfiles("prod");
    assertThrows(IllegalStateException.class, () -> new DevProfileGuard(env));
  }

  @Test
  void disabledTwoFactorIsAllowedOnlyInDevAndFailsInProduction() {
    var dev = new MockEnvironment().withProperty("auth.2fa-enabled", "false");
    dev.setActiveProfiles("dev");
    assertDoesNotThrow(() -> new DevProfileGuard(dev));

    var prod =
        new MockEnvironment()
            .withProperty("auth.2fa-enabled", "false")
            .withProperty("jwt.secret", "a-private-production-key-at-least-thirty-two-bytes")
            .withProperty("cors.allowed-origins", "https://mazlove.example")
            .withProperty("tracking.public-base-url", "https://mazlove.example");
    prod.setActiveProfiles("prod");
    assertThrows(IllegalStateException.class, () -> new DevProfileGuard(prod));
  }

  @Test
  void productionRequiresPrivateKeyAndExplicitHttpsOrigins() {
    var env =
        new MockEnvironment()
            .withProperty("jwt.secret", "a-private-production-key-at-least-thirty-two-bytes")
            .withProperty("cors.allowed-origins", "https://mazlove.example")
            .withProperty("tracking.public-base-url", "https://mazlove.example");
    env.setActiveProfiles("prod");
    assertDoesNotThrow(() -> new DevProfileGuard(env));
    env.setProperty("cors.allowed-origins", "https://*.example");
    assertThrows(IllegalStateException.class, () -> new DevProfileGuard(env));
    env.setProperty("cors.allowed-origins", "http://localhost:3000");
    assertThrows(IllegalStateException.class, () -> new DevProfileGuard(env));
    env.setProperty("cors.allowed-origins", "https://mazlove.example");
    env.setProperty("jwt.secret", "dev_secret_change_in_production_32chars");
    assertThrows(IllegalStateException.class, () -> new DevProfileGuard(env));
  }
}
