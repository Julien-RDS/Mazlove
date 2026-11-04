package com.mazlove.backend.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mazlove.backend.service.AuthService;
import com.mazlove.backend.service.MfaService;
import com.mazlove.backend.service.OidcAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class OidcLoginHandlersTest {
  @Test
  void redisFailureRedirectsWithoutJwtOrSession() throws Exception {
    var accounts = mock(OidcAccountService.class);
    var mfa = mock(MfaService.class);
    var auth = mock(AuthService.class);
    var principal = mock(OidcUser.class);
    when(accounts.requireExistingVerifiedEmail(principal)).thenReturn("alice@example.test");
    when(mfa.isEnabled()).thenReturn(true);
    when(mfa.sendOtp("alice@example.test")).thenReturn("a".repeat(64));
    doThrow(new org.springframework.data.redis.RedisConnectionFailureException("test"))
        .when(mfa)
        .sendOtp("alice@example.test");
    var request = new MockHttpServletRequest();
    request.getSession();
    var response = new MockHttpServletResponse();
    new OidcLoginHandlers(accounts, mfa, auth, "https://app.example.test")
        .success(request, response, new UsernamePasswordAuthenticationToken(principal, ""));
    assertNull(request.getSession(false));
    assertTrue(response.getRedirectedUrl().endsWith("oidcError=auth_unavailable"));
    verifyNoInteractions(auth);
  }

  @Test
  void successfulSsoRequiresLocalOtpBeforeIssuingAnyJwt() throws Exception {
    var accounts = mock(OidcAccountService.class);
    var mfa = mock(MfaService.class);
    var auth = mock(AuthService.class);
    var principal = mock(OidcUser.class);
    when(accounts.requireExistingVerifiedEmail(principal)).thenReturn("alice@example.test");
    when(mfa.isEnabled()).thenReturn(true);
    when(mfa.sendOtp("alice@example.test")).thenReturn("a".repeat(64));
    var request = new MockHttpServletRequest();
    request.getSession();
    var response = new MockHttpServletResponse();
    new OidcLoginHandlers(accounts, mfa, auth, "https://app.example.test")
        .success(request, response, new UsernamePasswordAuthenticationToken(principal, ""));
    verify(mfa).sendOtp("alice@example.test");
    verifyNoInteractions(auth);
    assertNull(request.getSession(false));
    assertEquals(
        "https://app.example.test/#/connexion?oidcEmail=alice%40example.test&challenge="
            + "a".repeat(64),
        response.getRedirectedUrl());
  }

  @Test
  void throttledOtpRedirectsWithoutIssuingJwt() throws Exception {
    var accounts = mock(OidcAccountService.class);
    var mfa = mock(MfaService.class);
    var auth = mock(AuthService.class);
    var principal = mock(OidcUser.class);
    when(accounts.requireExistingVerifiedEmail(principal)).thenReturn("alice@example.test");
    when(mfa.isEnabled()).thenReturn(true);
    when(mfa.sendOtp(anyString()))
        .thenThrow(
            new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.TOO_MANY_REQUESTS));
    var request = new MockHttpServletRequest();
    request.getSession();
    var response = new MockHttpServletResponse();
    new OidcLoginHandlers(accounts, mfa, auth, "https://app.example.test")
        .success(request, response, new UsernamePasswordAuthenticationToken(principal, ""));
    assertTrue(response.getRedirectedUrl().endsWith("oidcError=otp_throttled"));
    assertNull(request.getSession(false));
    verifyNoInteractions(auth);
  }
}
