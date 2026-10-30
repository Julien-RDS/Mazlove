package com.mazlove.backend.repository;

import com.mazlove.backend.models.Review;
import com.mazlove.backend.models.Session;
import com.mazlove.backend.models.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  @Query(
      value =
          """
SELECT s.* FROM sessions s JOIN providers p ON p.id=s.provider_id
WHERE ((:providerOnly=false AND s.client_id=:userId) OR p.user_id=:userId)
  AND ((:providerOnly=true AND s.status='EN_ROUTE') OR (:providerOnly=false AND
    (s.status IN ('EN_ROUTE','ARRIVED','IN_PROGRESS') OR
     (s.status IN ('ACCEPTED','PENDING') AND s.datetime + s.duration * INTERVAL '1 hour' > :now))))
ORDER BY CASE s.status WHEN 'EN_ROUTE' THEN 0 WHEN 'ARRIVED' THEN 1
  WHEN 'IN_PROGRESS' THEN 2 ELSE 3 END, s.datetime, s.id
LIMIT 1
""",
      nativeQuery = true)
  Optional<Session> findActiveSession(
      @Param("userId") Long userId,
      @Param("providerOnly") boolean providerOnly,
      @Param("now") java.time.LocalDateTime now);

  /**
   * Séances où l'utilisateur est participant, côté client OU côté praticien, de la plus lointaine
   * dans le futur à la plus ancienne. Paginé via le Pageable.
   */
  @Query(
      """
      SELECT s FROM Session s JOIN s.provider p
      WHERE s.client.id = :userId OR p.user.id = :userId
      ORDER BY s.datetime DESC
      """)
  List<Session> findSessionsByUserId(@Param("userId") Long userId, Pageable pageable);

  /** Avis rédigés par l'utilisateur, du plus récent au plus ancien. Paginé via le Pageable. */
  @Query("SELECT r FROM Review r WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
  List<Review> findReviewsByUserId(@Param("userId") Long userId, Pageable pageable);
}
