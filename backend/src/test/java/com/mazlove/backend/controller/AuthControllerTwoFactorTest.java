package com.mazlove.backend.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mazlove.backend.config.OidcSettings;
import com.mazlove.backend.dto.LoginDTO;
import com.mazlove.backend.service.AuthService;
import com.mazlove.backend.service.MfaService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class AuthControllerTwoFactorTest {
  @Test
  void devWithTwoFactorDisabledSkipsOtpAndReturnsJwt() {
    AuthService auth = mock(AuthService.class);
    MfaService mfa = mock(MfaService.class);
    when(mfa.isEnabled()).thenReturn(false);
    when(auth.generateJwt("alice@example.com")).thenReturn("jwt-dev");
    var controller = new AuthController(auth, mfa, new OidcSettings(new MockEnvironment()));

    var result = controller.login(new LoginDTO("alice@example.com", "secret")).getBody();

    assertNotNull(result);
    assertEquals("jwt-dev", result.token());
    assertFalse(result.mfaRequired());
    verify(mfa, never()).sendOtp(anyString());
  }

  @Test
  void devWithTwoFactorEnabledRequiresOtp() {
    AuthService auth = mock(AuthService.class);
    MfaService mfa = mock(MfaService.class);
    when(mfa.isEnabled()).thenReturn(true);
    var controller = new AuthController(auth, mfa, new OidcSettings(new MockEnvironment()));

    var result = controller.login(new LoginDTO("alice@example.com", "secret")).getBody();

    assertNotNull(result);
    assertNull(result.token());
    assertTrue(result.mfaRequired());
    verify(mfa).sendOtp("alice@example.com");
  }
}
