package com.mazlove.backend.repository;

import com.mazlove.backend.models.Favorite;
import com.mazlove.backend.models.FavoriteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {
    @Query("select f from Favorite f join fetch f.provider p join fetch p.user where f.user.id = :userId order by p.id")
    List<Favorite> findByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndProviderId(Long userId, Long providerId);

    @Modifying
    @Query(value = "INSERT INTO favorites (user_id, provider_id) VALUES (:userId, :providerId) ON CONFLICT DO NOTHING", nativeQuery = true)
    int addIfAbsent(@Param("userId") Long userId, @Param("providerId") Long providerId);

    @Modifying
    @Query("delete from Favorite f where f.user.id = :userId and f.provider.id = :providerId")
    int remove(@Param("userId") Long userId, @Param("providerId") Long providerId);
}
