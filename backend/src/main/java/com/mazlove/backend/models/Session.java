package com.mazlove.backend.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Séance réservée par un client auprès d'un praticien.
 *
 * Cycle de vie (appliqué par SessionService) :
 * <pre>
 *   PENDING ──(le praticien saisit le code d'arrivée)──> ACCEPTED ──(fin confirmée)──> DONE
 *      └──────────────(annulation par l'un ou l'autre)──> CANCELLED
 * </pre>
 * Le code d'arrivée est remis au client à la réservation ; il le donne au praticien
 * en personne, ce qui prouve que les deux sont bien au rendez-vous.
 */
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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /** Fin prévue de la séance, utile pour détecter les chevauchements. */
    public LocalDateTime getEndTime() {
        return datetime.plusHours(duration);
    }

    /**
     * PENDING : réservée, en attente de l'arrivée.
     * ACCEPTED : code d'arrivée validé, séance en cours.
     * DONE : terminée, un avis peut être laissé.
     * CANCELLED : annulée avant de commencer.
     */
    public enum Status {
        PENDING, ACCEPTED, DONE, CANCELLED
    }
}
