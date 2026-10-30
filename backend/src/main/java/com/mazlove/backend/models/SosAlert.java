package com.mazlove.backend.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sos_alerts")
@Getter
@Setter
@NoArgsConstructor
public class SosAlert {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long sessionId;
  private Long raisedBy;
  private Double latitude;
  private Double longitude;
  private String status;
  private java.time.LocalDateTime createdAt;
  private java.time.LocalDateTime resolvedAt;
}
