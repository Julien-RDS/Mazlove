package com.mazlove.backend.repository;

import com.mazlove.backend.models.Review;
import com.mazlove.backend.models.Session;
import com.mazlove.backend.models.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Séances où l'utilisateur est participant, côté client OU côté praticien,
     * de la plus lointaine dans le futur à la plus ancienne. Paginé via le Pageable.
     */
    @Query("""
            SELECT s FROM Session s JOIN s.provider p
            WHERE s.client.id = :userId OR p.user.id = :userId
            ORDER BY s.datetime DESC
            """)
    List<Session> findSessionsByUserId(@Param("userId") Long userId, Pageable pageable);

    /** Avis rédigés par l'utilisateur, du plus récent au plus ancien. Paginé via le Pageable. */
    @Query("SELECT r FROM Review r WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    List<Review> findReviewsByUserId(@Param("userId") Long userId, Pageable pageable);
}
