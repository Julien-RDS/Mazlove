package com.mazlove.backend.repository;

import com.mazlove.backend.models.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long> {

    /*
     * Recherche géographique en deux temps, dans la même requête :
     *  1. un filtre "boîte englobante" (BETWEEN sur latitude/longitude) qui exploite l'index
     *     idx_providers_location et écarte d'emblée 99 % des lignes ;
     *  2. la distance exacte (formule de Haversine, rayon terrestre 6371 km) sur les lignes restantes.
     * LEAST/GREATEST bornent l'argument de acos() à [-1, 1] : sans cela, les arrondis flottants
     * produisent parfois 1.0000000002 et PostgreSQL lève "input is out of range".
     * Les bornes de la boîte sont calculées côté Java (GeoUtils.boundingBox) ; le rayon vaut
     * toujours 50 km (ProviderService.SEARCH_RADIUS_KM), comme dans la requête d'origine.
     * Pagination via LIMIT/OFFSET explicites : plus prévisible qu'un Pageable sur requête native.
     */
    @Query(value = """
            SELECT p.* FROM providers p
            WHERE p.latitude BETWEEN :minLat AND :maxLat
              AND p.longitude BETWEEN :minLng AND :maxLng
              AND (6371 * acos(LEAST(1.0, GREATEST(-1.0,
                    cos(radians(:lat)) * cos(radians(p.latitude))
                    * cos(radians(p.longitude) - radians(:lng))
                    + sin(radians(:lat)) * sin(radians(p.latitude)))))) <= :radiusKm
            ORDER BY (6371 * acos(LEAST(1.0, GREATEST(-1.0,
                    cos(radians(:lat)) * cos(radians(p.latitude))
                    * cos(radians(p.longitude) - radians(:lng))
                    + sin(radians(:lat)) * sin(radians(p.latitude)))))), p.id
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Provider> findNearby(@Param("lat") double lat,
                              @Param("lng") double lng,
                              @Param("radiusKm") double radiusKm,
                              @Param("minLat") double minLat,
                              @Param("maxLat") double maxLat,
                              @Param("minLng") double minLng,
                              @Param("maxLng") double maxLng,
                              @Param("limit") int limit,
                              @Param("offset") int offset);

    /**
     * Praticiens dans le rayon qui ont au moins un créneau le jour demandé
     * (format java.time.DayOfWeek : "MONDAY", "TUESDAY"…). EXISTS plutôt qu'un JOIN + DISTINCT :
     * un praticien avec deux créneaux le même jour n'apparaît qu'une fois, sans tri coûteux.
     */
    @Query(value = """
            SELECT p.* FROM providers p
            WHERE p.latitude BETWEEN :minLat AND :maxLat
              AND p.longitude BETWEEN :minLng AND :maxLng
              AND EXISTS (SELECT 1 FROM provider_availability pa
                          WHERE pa.provider_id = p.id AND pa.day_of_week = :dayOfWeek)
              AND (6371 * acos(LEAST(1.0, GREATEST(-1.0,
                    cos(radians(:lat)) * cos(radians(p.latitude))
                    * cos(radians(p.longitude) - radians(:lng))
                    + sin(radians(:lat)) * sin(radians(p.latitude)))))) <= :radiusKm
            ORDER BY p.id
            LIMIT 50
            """, nativeQuery = true)
    List<Provider> findAvailableProviders(@Param("lat") double lat,
                                          @Param("lng") double lng,
                                          @Param("radiusKm") double radiusKm,
                                          @Param("minLat") double minLat,
                                          @Param("maxLat") double maxLat,
                                          @Param("minLng") double minLng,
                                          @Param("maxLng") double maxLng,
                                          @Param("dayOfWeek") String dayOfWeek);

    /** Un compte ne peut avoir qu'une fiche praticien. */
    boolean existsByUser_Id(Long userId);

    /** Vérifie qu'un praticien appartient bien au compte connecté (WebSocket, droits). */
    boolean existsByIdAndUser_Email(Long id, String email);

    /** Id de la fiche praticien d'un compte, sans charger l'entité. */
    @Query("SELECT p.id FROM Provider p WHERE p.user.email = :email")
    Optional<Long> findIdByUserEmail(@Param("email") String email);
}
