package com.mazlove.backend.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tracking_events")
@Getter
@Setter
@NoArgsConstructor
public class TrackingEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long sessionId;
  private String eventType;
  private Double latitude;
  private Double longitude;
  private String details;
  private java.time.LocalDateTime createdAt;
}
