package com.mazlove.backend.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** Évite de créer une inscription OAuth partielle lorsqu'une des trois variables manque. */
public class OidcEnabledCondition implements Condition {
  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    var environment = context.getEnvironment();
    return hasText(environment.getProperty("auth.oidc.issuer"))
        && hasText(environment.getProperty("auth.oidc.client-id"))
        && hasText(environment.getProperty("auth.oidc.client-secret"));
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
