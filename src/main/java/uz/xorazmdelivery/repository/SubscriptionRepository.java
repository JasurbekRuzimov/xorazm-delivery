package uz.xorazmdelivery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.xorazmdelivery.entity.Subscription;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    @Query("""
        SELECT s FROM Subscription s
        WHERE s.user.id = :userId AND s.active = TRUE
        AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)
        ORDER BY s.startedAt DESC
        """)
    Optional<Subscription> findActiveByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.plan = 'PREMIUM' AND s.active = TRUE")
    long countActivePremium();

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.plan = 'BUSINESS' AND s.active = TRUE")
    long countActiveBusiness();
}
