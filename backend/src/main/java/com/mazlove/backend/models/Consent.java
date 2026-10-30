package com.mazlove.backend.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "consents")
@Getter
@Setter
@NoArgsConstructor
public class Consent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long userId;
  private String scope;
  private String policyVersion;
  private java.time.LocalDateTime grantedAt;
  private java.time.LocalDateTime revokedAt;
}
