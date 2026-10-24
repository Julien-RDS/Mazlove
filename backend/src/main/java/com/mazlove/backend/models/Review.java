package com.mazlove.backend.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Avis laissé par un client sur une séance terminée (une seule fois par séance).
 *
 * Chaîne de relations : Review -> Session -> Provider. Le provider est en plus
 * dénormalisé dans reviews.provider_id : la liste des avis et la note moyenne d'un
 * praticien se calculent ainsi sans double jointure, et Provider#reviews (mappedBy
 * "provider") pointe enfin vers un champ qui existe.
 */
@Data
@Entity
@Table(name = "reviews")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"user", "session", "provider"})
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Auteur de l'avis : toujours le client de la séance (vérifié dans ReviewService). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    /** Copie de session.provider, renseignée à la création de l'avis. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    /** Note de 1 à 5 (contrainte CHECK côté SQL, @Min/@Max côté DTO). */
    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
