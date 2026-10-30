package com.mazlove.backend.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Séance : acceptation, trajet, arrivée, code, séance en cours, fin. */
@Data
@Entity
@Table(name = "sessions")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"client", "provider", "codeValidation"})
public class Session {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "client_id", nullable = false)
  private User client;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "provider_id", nullable = false)
  private Provider provider;

  /** Date et heure de début (heure locale, sans fuseau). */
  @Column(nullable = false)
  private LocalDateTime datetime;

  /** Durée en heures, de 1 à 12. */
  @Column(nullable = false)
  private int duration = 1;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "service_item_id")
  private ServiceItem serviceItem;

  /** Prix convenu au moment de la réservation, indépendant du tarif futur. */
  @Column(name = "price_snapshot")
  private Float priceSnapshot;

  @Enumerated(EnumType.STRING)
  private Status status;

  /** Code à 6 caractères remis au client, à ne jamais montrer au praticien. */
  @Column(nullable = false, length = 6)
  private String codeValidation;

  /** Consignes du client (limites, préférences), visibles par le praticien. */
  @Column(columnDefinition = "TEXT")
  private String notes;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Version private Long version;
  private Double meetingLatitude;
  private Double meetingLongitude;
  private String meetingLabel;

  @Enumerated(EnumType.STRING)
  private com.mazlove.backend.tracking.TravelMode travelMode;

  private LocalDateTime enRouteAt;
  private LocalDateTime arrivedAt;
  private LocalDateTime startedAt;
  private LocalDateTime endedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }

  /** Fin prévue de la séance, utile pour détecter les chevauchements. */
  public LocalDateTime getEndTime() {
    return datetime.plusHours(duration);
  }

  /**
   * PENDING : réservée, en attente de l'arrivée. ACCEPTED : réservation acceptée, déplacement pas
   * encore commencé. DONE : terminée, un avis peut être laissé. CANCELLED : annulée avant de
   * commencer.
   */
  public enum Status {
    PENDING,
    ACCEPTED,
    EN_ROUTE,
    ARRIVED,
    IN_PROGRESS,
    DONE,
    CANCELLED
  }
}
