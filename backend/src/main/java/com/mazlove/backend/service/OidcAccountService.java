package com.mazlove.backend.service;

import com.mazlove.backend.repository.UserRepository;
import com.mazlove.backend.util.Emails;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/** Rattache le SSO à un compte existant uniquement lorsque le fournisseur a vérifié l'email. */
@Service
public class OidcAccountService {
  private final UserRepository users;

  public OidcAccountService(UserRepository users) {
    this.users = users;
  }

  public String requireExistingVerifiedEmail(OidcUser user) {
    if (!Boolean.TRUE.equals(user.getEmailVerified())) {
      throw rejected("email_not_verified", "Le fournisseur n'a pas vérifié cette adresse email.");
    }
    String email = user.getEmail();
    if (email == null || email.isBlank()) {
      throw rejected("email_missing", "Le fournisseur n'a pas transmis d'adresse email.");
    }
    String normalized = Emails.normalize(email);
    if (users.findByEmail(normalized).isEmpty()) {
      throw rejected(
          "account_not_found", "Aucun compte Mazlove existant ne correspond à cette adresse.");
    }
    return normalized;
  }

  private static OAuth2AuthenticationException rejected(String code, String description) {
    return new OAuth2AuthenticationException(new OAuth2Error(code, description, null));
  }
}
