package com.mazlove.backend.config;

import com.mazlove.backend.service.AuthService;
import com.mazlove.backend.service.MfaService;
import com.mazlove.backend.service.OidcAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/** Termine le SSO par l'OTP local, ou par un JWT direct lorsque la 2FA est coupée en dev. */
@Component
public class OidcLoginHandlers {
  private final OidcAccountService accounts;
  private final MfaService mfa;
  private final AuthService auth;
  private final String publicBaseUrl;

  public OidcLoginHandlers(
      OidcAccountService accounts,
      MfaService mfa,
      AuthService auth,
      @Value("${tracking.public-base-url:http://localhost:3000}") String publicBaseUrl) {
    this.accounts = accounts;
    this.mfa = mfa;
    this.auth = auth;
    this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
  }

  public void success(
      HttpServletRequest request, HttpServletResponse response, Authentication result)
      throws IOException {
    String email = accounts.requireExistingVerifiedEmail((OidcUser) result.getPrincipal());
    clearSession(request);
    if (mfa.isEnabled()) {
      try {
        mfa.sendOtp(email);
      } catch (org.springframework.mail.MailException e) {
        redirect(response, "oidcError", "mail_unavailable");
        return;
      }
      redirect(response, "oidcEmail", email);
    } else {
      redirect(response, "oidcToken", auth.generateJwt(email));
    }
  }

  public void failure(HttpServletRequest request, HttpServletResponse response, Exception exception)
      throws IOException {
    clearSession(request);
    String code = "refused";
    if (exception
        instanceof org.springframework.security.oauth2.core.OAuth2AuthenticationException e)
      code = e.getError().getErrorCode();
    redirect(response, "oidcError", code);
  }

  private void redirect(HttpServletResponse response, String key, String value) throws IOException {
    response.sendRedirect(
        publicBaseUrl
            + "/#/connexion?"
            + key
            + "="
            + URLEncoder.encode(value, StandardCharsets.UTF_8));
  }

  private static void clearSession(HttpServletRequest request) {
    var session = request.getSession(false);
    if (session != null) session.invalidate();
  }
}
