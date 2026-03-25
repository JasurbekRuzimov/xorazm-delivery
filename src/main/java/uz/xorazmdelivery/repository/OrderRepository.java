package uz.xorazmdelivery.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.xorazmdelivery.entity.Order;
import uz.xorazmdelivery.enums.OrderStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    Page<Order> findByCourierIdOrderByCreatedAtDesc(UUID courierId, Pageable pageable);

    List<Order> findByStatus(OrderStatus status);

    Optional<Order> findByIdAndCustomerId(UUID id, UUID customerId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.courier.id = :courierId " +
           "AND o.status = uz.xorazmdelivery.enums.OrderStatus.DELIVERED " +
           "AND CAST(o.deliveredAt AS date) = CURRENT_DATE")
    Long countTodayDeliveredByCourier(@Param("courierId") UUID courierId);

    @Query("SELECT COALESCE(SUM(o.totalFee), 0) FROM Order o " +
           "WHERE o.courier.id = :courierId AND o.status = uz.xorazmdelivery.enums.OrderStatus.DELIVERED " +
           "AND CAST(o.deliveredAt AS date) = CURRENT_DATE")
    Long sumTodayEarningsByCourier(@Param("courierId") UUID courierId);

    @Modifying
    @Query(value = "UPDATE orders SET status = CAST(:status AS order_status), updated_at = NOW() WHERE id = CAST(:id AS uuid)",
           nativeQuery = true)
    void updateStatus(@Param("id") String id, @Param("status") String status);
}
