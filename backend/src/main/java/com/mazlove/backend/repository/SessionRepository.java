package com.mazlove.backend.repository;

import com.mazlove.backend.models.Session;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
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

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from Session s where s.id=:id")
  java.util.Optional<Session> lockById(@Param("id") Long id);

  /**
   * Séances actives d'un praticien commençant dans une fenêtre donnée. SessionService s'en sert
   * pour refuser une réservation qui chevaucherait une autre : la fenêtre est élargie de 12 h
   * (durée max) en amont pour attraper une séance commencée plus tôt et encore en cours.
   */
  List<Session> findByProvider_IdAndStatusInAndDatetimeBetween(
      Long providerId, Collection<Session.Status> statuses, LocalDateTime from, LocalDateTime to);

  @Query(
      "SELECT s FROM Session s WHERE s.provider.id = :providerId AND s.status IN :statuses AND"
          + " s.datetime > :now ORDER BY s.datetime ASC")
  List<Session> findUpcoming(
      @Param("providerId") Long providerId,
      @Param("statuses") Collection<Session.Status> statuses,
      @Param("now") LocalDateTime now);

  @Query(
      "SELECT s FROM Session s WHERE s.provider.id = :providerId AND s.status IN :statuses ORDER BY"
          + " s.datetime DESC")
  List<Session> findHistory(
      @Param("providerId") Long providerId, @Param("statuses") Collection<Session.Status> statuses);

  long countByProvider_Id(Long providerId);

  long countByProvider_IdAndStatus(Long providerId, Session.Status status);

  @Query(
      "SELECT SUM(s.priceSnapshot) FROM Session s WHERE s.provider.id = :providerId AND s.status ="
          + " :status")
  Double sumRecordedPrices(
      @Param("providerId") Long providerId, @Param("status") Session.Status status);

  long countByProvider_IdAndStatusAndPriceSnapshotIsNull(Long providerId, Session.Status status);
}
