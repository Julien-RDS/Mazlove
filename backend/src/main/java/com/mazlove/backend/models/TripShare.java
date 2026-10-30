package com.mazlove.backend.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trip_shares")
@Getter
@Setter
@NoArgsConstructor
public class TripShare {
  @Id private String token;
  private Long sessionId;
  private Long createdBy;
  private java.time.LocalDateTime expiresAt;
  private boolean revoked;
}
