package uz.xorazmdelivery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.xorazmdelivery.entity.Payment;
import uz.xorazmdelivery.enums.PaymentStatus;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByOrderId(UUID orderId);

    boolean existsByOrderIdAndStatus(UUID orderId, PaymentStatus status);
}
