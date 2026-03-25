package uz.xorazmdelivery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.xorazmdelivery.entity.CourierProfile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourierProfileRepository extends JpaRepository<CourierProfile, UUID> {

    Optional<CourierProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    /**
     * Radiusda online kuryerlarni masofaga qarab tartiblangan holda topadi.
     * PostGIS ST_Distance ishlatiladi (WGS84 degrees -> approximately km * 111.32).
     * Skor = reyting * 0.6 + (1/masofa) * 0.4
     */
    @Query(value = """
        SELECT cp.* FROM courier_profiles cp
        JOIN users u ON cp.user_id = u.id
        WHERE cp.is_online = TRUE
          AND cp.is_verified = TRUE
          AND u.is_active = TRUE
          AND cp.current_lat IS NOT NULL
          AND cp.current_lng IS NOT NULL
          AND (
              6371 * acos(
                  cos(radians(:lat)) * cos(radians(cp.current_lat)) *
                  cos(radians(cp.current_lng) - radians(:lng)) +
                  sin(radians(:lat)) * sin(radians(cp.current_lat))
              )
          ) <= :radiusKm
        ORDER BY (
            CAST(cp.rating AS FLOAT) * 0.6 +
            (1.0 / GREATEST(
                6371 * acos(
                    cos(radians(:lat)) * cos(radians(cp.current_lat)) *
                    cos(radians(cp.current_lng) - radians(:lng)) +
                    sin(radians(:lat)) * sin(radians(cp.current_lat))
                ), 0.1
            )) * 0.4
        ) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<CourierProfile> findNearbyCouriers(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm,
            @Param("limit") int limit
    );

    @Modifying
    @Query("UPDATE CourierProfile cp SET cp.online = :online WHERE cp.user.id = :userId")
    void updateOnlineStatus(@Param("userId") UUID userId, @Param("online") boolean online);

    @Modifying
    @Query("""
        UPDATE CourierProfile cp
        SET cp.currentLat = :lat, cp.currentLng = :lng, cp.locationUpdatedAt = :updatedAt
        WHERE cp.user.id = :userId
        """)
    void updateLocation(
            @Param("userId") UUID userId,
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("updatedAt") Instant updatedAt
    );
}
