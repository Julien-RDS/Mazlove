package com.mazlove.backend.repository;

import com.mazlove.backend.models.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Avis d'un praticien, du plus récent au plus ancien.
     * Passe par la colonne dénormalisée reviews.provider_id ; l'équivalent sans
     * dénormalisation serait "r.session.provider.id", qui coûte deux jointures.
     */
    @Query("SELECT r FROM Review r WHERE r.provider.id = :providerId ORDER BY r.createdAt DESC")
    List<Review> findByProviderId(@Param("providerId") Long providerId);

    @Query("SELECT r FROM Review r WHERE r.session.id = :sessionId")
    Optional<Review> findBySessionId(@Param("sessionId") Long sessionId);

    boolean existsBySession_Id(Long sessionId);

    /** Parmi ces séances, lesquelles ont déjà un avis ? (une seule requête pour toute une liste) */
    @Query("SELECT r.session.id FROM Review r WHERE r.session.id IN :sessionIds")
    List<Long> findReviewedSessionIds(@Param("sessionIds") Collection<Long> sessionIds);

    /**
     * Note moyenne et nombre d'avis pour plusieurs praticiens en une requête
     * (évite une requête par carte dans la liste). Les alias correspondent aux
     * getters de la projection {@link RatingSummary}.
     * Ne pas appeler avec une collection vide : ProviderService le vérifie avant.
     */
    @Query("""
            SELECT r.provider.id AS providerId, AVG(r.rating) AS average, COUNT(r) AS total
            FROM Review r
            WHERE r.provider.id IN :providerIds
            GROUP BY r.provider.id
            """)
    List<RatingSummary> summarizeByProviderIds(@Param("providerIds") Collection<Long> providerIds);

    /** Projection en lecture seule du résultat agrégé ci-dessus. */
    interface RatingSummary {
        Long getProviderId();
        Double getAverage();
        Long getTotal();
    }
}
