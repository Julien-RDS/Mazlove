package com.mazlove.backend.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;

import java.util.Date;
import java.util.Set;

/**
 * Compte utilisateur : client qui réserve des séances, praticien qui en propose, ou admin.
 *
 * Un praticien possède en plus une fiche {@link Provider} (relation 1-1 côté métier,
 * garantie par l'index unique uq_providers_user du schéma SQL).
 */
@Data
@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
// equals/hashCode sur l'id uniquement : comparer les collections ferait charger
// toutes les relations LAZY (et boucler à l'infini via Provider -> User -> Provider).
@EqualsAndHashCode(of = "id")
// Le mot de passe (même haché) ne doit jamais finir dans un log via toString().
@ToString(exclude = {"password", "providers", "reviews", "sessions"})
// Charge les utilisateurs par paquets de 20 quand on parcourt une liste de séances/avis
// (évite une requête SQL par ligne pour afficher les noms).
@BatchSize(size = 20)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant de connexion, toujours stocké en minuscules (cf. AuthService). */
    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    /** Hash BCrypt, jamais le mot de passe en clair. */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    private Role role;

    private float rating;

    /** URL ou data-URI de l'avatar. TEXT et non @Lob : sous PostgreSQL, @Lob crée un OID. */
    @Column(columnDefinition = "TEXT")
    private String avatar;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Provider> providers;

    /** Avis rédigés par cet utilisateur (en tant que client). */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Review> reviews;

    /** Séances réservées par cet utilisateur (en tant que client). */
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Session> sessions;

    @Column(name = "created_at", updatable = false)
    private Date createdAt = new Date();

    @Column(name = "updated_at")
    private Date updatedAt = new Date();

    /**
     * CLIENT et PROVIDER sont choisis à l'inscription.
     * ADMIN n'est jamais attribuable via l'API publique (RegisterDTO le refuse).
     */
    public enum Role {
        CLIENT, PROVIDER, ADMIN
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
