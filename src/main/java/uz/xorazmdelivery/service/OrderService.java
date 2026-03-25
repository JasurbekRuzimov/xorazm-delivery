package uz.xorazmdelivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.xorazmdelivery.dto.request.CreateOrderRequest;
import uz.xorazmdelivery.dto.request.UpdateOrderStatusRequest;
import uz.xorazmdelivery.entity.Order;
import uz.xorazmdelivery.entity.User;
import uz.xorazmdelivery.enums.OrderStatus;
import uz.xorazmdelivery.exception.BusinessException;
import uz.xorazmdelivery.exception.ForbiddenException;
import uz.xorazmdelivery.exception.ResourceNotFoundException;
import uz.xorazmdelivery.repository.OrderRepository;
import uz.xorazmdelivery.repository.UserRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final String COURIER_ASSIGNMENT_KEY = "order:assignment:";

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PriceCalculatorService priceService;
    private final DeliveryAssignmentService assignmentService;
    private final NotificationService notificationService;
    private final StringRedisTemplate redis;

    /**
     * Yangi buyurtma yaratish.
     */
    @Transactional
    public Order createOrder(UUID customerId, CreateOrderRequest req) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Foydalanuvchi", customerId));

        double distance = priceService.calculateDistance(
                req.getPickupLat(), req.getPickupLng(),
                req.getDeliveryLat(), req.getDeliveryLng());

        var priceResult = priceService.calculate(
                distance, req.getWeightKg().doubleValue(), customer);

        Order order = Order.builder()
                .customer(customer)
                .pickupAddress(req.getPickupAddress())
                .pickupLat(req.getPickupLat())
                .pickupLng(req.getPickupLng())
                .deliveryAddress(req.getDeliveryAddress())
                .deliveryLat(req.getDeliveryLat())
                .deliveryLng(req.getDeliveryLng())
                .distanceKm(java.math.BigDecimal.valueOf(distance).setScale(2, java.math.RoundingMode.HALF_UP))
                .weightKg(req.getWeightKg())
                .fragile(Boolean.TRUE.equals(req.getFragile()))
                .description(req.getDescription())
                .totalFee(priceResult.totalFee())
                .nightRate(priceResult.nightRate())
                .status(OrderStatus.PENDING)
                .build();

        order = orderRepository.save(order);

        log.info("Buyurtma yaratildi: {} — {} so'm", order.getId(), order.getTotalFee());

        // Asinxron kuryer qidirish
        assignmentService.startSearch(order.getId());

        notificationService.notifyOrderCreated(order);

        return order;
    }

    /**
     * Buyurtma holatini yangilash (kuryer tomonidan).
     */
    @Transactional
    public Order updateStatus(UUID orderId, UUID courierId, UpdateOrderStatusRequest req) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyurtma", orderId));

        // Faqat tayinlangan kuryer o'zgartirishi mumkin
        if (order.getCourier() == null || !order.getCourier().getId().equals(courierId)) {
            throw new ForbiddenException("Bu buyurtmani o'zgartirish huquqingiz yo'q");
        }

        OrderStatus newStatus = req.getStatus();
        validateStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);
        switch (newStatus) {
            case PICKED_UP    -> order.setPickedUpAt(Instant.now());
            case ON_THE_WAY   -> { /* no-op, timestamp already set */ }
            case DELIVERED    -> {
                order.setDeliveredAt(Instant.now());
                notificationService.notifyOrderDelivered(order);
            }
            default -> {}
        }

        order = orderRepository.save(order);
        log.info("Buyurtma {} holati -> {}", orderId, newStatus);
        return order;
    }

    /**
     * Buyurtmani bekor qilish (faqat PENDING/SEARCHING holatida).
     */
    @Transactional
    public void cancelOrder(UUID orderId, UUID customerId, String reason) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyurtma", orderId));

        if (order.getStatus() == OrderStatus.PICKED_UP ||
            order.getStatus() == OrderStatus.ON_THE_WAY ||
            order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessException("Yetkazilayotgan buyurtmani bekor qilib bo'lmaydi");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        order.setCancelReason(reason);
        orderRepository.save(order);

        log.info("Buyurtma {} bekor qilindi: {}", orderId, reason);
    }

    @Transactional(readOnly = true)
    public Order getOrderById(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyurtma", orderId));

        boolean isCustomer = order.getCustomer().getId().equals(userId);
        boolean isCourier  = order.getCourier() != null && order.getCourier().getId().equals(userId);

        if (!isCustomer && !isCourier) {
            throw new ForbiddenException("Bu buyurtmani ko'rish huquqingiz yo'q");
        }

        return order;
    }

    @Transactional(readOnly = true)
    public Page<Order> getMyOrders(UUID customerId, Pageable pageable) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
    }

    // ─── private ──────────────────────────────────────────────────

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case ASSIGNED   -> next == OrderStatus.PICKED_UP || next == OrderStatus.CANCELLED;
            case PICKED_UP  -> next == OrderStatus.ON_THE_WAY;
            case ON_THE_WAY -> next == OrderStatus.DELIVERED;
            default         -> false;
        };
        if (!valid) {
            throw new BusinessException("Noto'g'ri holat o'tishi: " + current + " → " + next);
        }
    }
}
