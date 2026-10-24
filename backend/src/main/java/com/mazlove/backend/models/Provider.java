package com.mazlove.backend.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Fiche publique d'un praticien : présentation, zone d'intervention et créneaux hebdomadaires.
 *
 * La note moyenne n'est PAS stockée ici : elle est calculée à la volée à partir des avis
 * (ReviewRepository#summarizeByProviderIds), ce qui évite toute désynchronisation.
 */
@Data
@Entity
@Table(name = "providers")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"user", "services", "sessions", "reviews"})
public class Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Compte propriétaire de la fiche (son nom sert de nom d'affichage). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String bio;

    /** Type de séance proposée, en texte libre (ex. « Câlins assis », « Cuillère »). */
    @Column(name = "category", nullable = false, length = 100)
    private String category;

    /** Badge de confiance : seul un admin peut le passer à true, jamais le praticien lui-même. */
    @Column(nullable = false)
    private boolean verified = false;

    @ElementCollection
    @CollectionTable(name = "provider_images", joinColumns = @JoinColumn(name = "provider_id"))
    @Column(name = "image_url")
    @BatchSize(size = 20)
    private Set<String> images = new HashSet<>();

    /** Colonnes latitude/longitude directement dans la table providers. */
    @Embedded
    private Location location;

    @ElementCollection
    @CollectionTable(name = "provider_availability", joinColumns = @JoinColumn(name = "provider_id"))
    @BatchSize(size = 20)
    private Set<TimeSlot> availability = new HashSet<>();

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ServiceItem> services = new HashSet<>();

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Session> sessions = new HashSet<>();

    /** Relation inverse de Review#provider (colonne reviews.provider_id). */
    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Review> reviews = new HashSet<>();

    @Column(name = "created_at", updatable = false)
    private Date createdAt = new Date();

    @Column(name = "updated_at")
    private Date updatedAt = new Date();

    /**
     * Position du praticien. Elle n'est jamais renvoyée telle quelle au public :
     * ProviderService l'arrondit à ~1 km pour ne pas révéler une adresse précise.
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location {
        private double latitude;
        private double longitude;
    }

    /**
     * Créneau récurrent : jour en anglais majuscule (format java.time.DayOfWeek, ex. "MONDAY")
     * et heures au format "HH:mm" (normalisées à l'enregistrement, ex. "09:00").
     * Stocké en VARCHAR : la comparaison de chaînes "HH:mm" respecte l'ordre chronologique.
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlot {
        private String dayOfWeek;
        private String startTime;
        private String endTime;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
}
