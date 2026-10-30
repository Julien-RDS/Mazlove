package com.mazlove.backend.tracking;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.env.MockEnvironment;

class DevProfileGuardTest {
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
}
