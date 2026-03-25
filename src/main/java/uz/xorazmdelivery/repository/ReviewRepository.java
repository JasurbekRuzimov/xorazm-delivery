package uz.xorazmdelivery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.xorazmdelivery.entity.Review;

import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    boolean existsByOrderId(UUID orderId);
}
