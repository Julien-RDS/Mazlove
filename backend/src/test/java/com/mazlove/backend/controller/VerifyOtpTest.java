package com.mazlove.backend.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.mazlove.backend.config.GlobalExceptionHandler;
import com.mazlove.backend.config.OidcSettings;
import com.mazlove.backend.service.AuthService;
import com.mazlove.backend.service.MfaService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class VerifyOtpTest {
  @Test
  void validCodeReturnsJwt() throws Exception {
    verifyEndpoint(true);
  }

  @Test
  void rejectedCodeReturns401AndNeverIssuesJwt() throws Exception {
    verifyEndpoint(false);
  }

  void verifyEndpoint(boolean valid) throws Exception {
    var mfa = mock(MfaService.class);
    var auth = mock(AuthService.class);
    when(mfa.verifyOtp(any())).thenReturn(valid);
    when(auth.generateJwt("alice@example.test")).thenReturn("test-jwt");
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new AuthController(auth, mfa, new OidcSettings(new MockEnvironment())))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    var result =
        mvc.perform(
            post("/api/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"alice@example.test\",\"code\":\"123456\"}"));
    if (valid) result.andExpect(status().isOk()).andExpect(jsonPath("$.token").value("test-jwt"));
    else {
      result.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.token").doesNotExist());
      verify(auth, never()).generateJwt(anyString());
    }
  }
}
