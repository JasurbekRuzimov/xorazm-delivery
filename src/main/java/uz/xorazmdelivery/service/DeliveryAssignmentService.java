package uz.xorazmdelivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.xorazmdelivery.entity.CourierProfile;
import uz.xorazmdelivery.entity.Order;
import uz.xorazmdelivery.enums.OrderStatus;
import uz.xorazmdelivery.repository.CourierProfileRepository;
import uz.xorazmdelivery.repository.OrderRepository;
import uz.xorazmdelivery.repository.UserRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Kuryer tayinlash algoritmi (TZ 5.3 asosida):
 * 1. Pickup manzilidan 3 km radiusda online kuryerlarni qidiradi (PostGIS)
 * 2. Skor = reyting * 0.6 + (1/masofa) * 0.4
 * 3. Eng yuqori skor olganga 60 soniyalik so'rov yuboriladi
 * 4. Rad etsa/javob bermasa — keyingisiga o'tiladi
 * 5. 5 km — 7 km ga kengaytiriladi agar topilmasa
 * 6. 10 daqiqada topilmasa — mijozga SMS
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryAssignmentService {

    private static final String PENDING_OFFER_KEY = "courier:offer:";  // orderId -> courierId
    private static final String ORDER_SEARCH_KEY  = "order:searching:"; // orderId -> "1"

    @Value("${delivery.courier-search-radius-km:3}")
    private double searchRadiusKm;

    @Value("${delivery.courier-search-max-radius-km:7}")
    private double maxSearchRadiusKm;

    @Value("${delivery.courier-accept-timeout-sec:60}")
    private long acceptTimeoutSec;

    @Value("${delivery.max-assignment-wait-min:10}")
    private long maxWaitMin;

    private final OrderRepository orderRepository;
    private final CourierProfileRepository courierProfileRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final StringRedisTemplate redis;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    /**
     * Asinxron kuryer qidirish boshlash.
     */
    @Async
    public void startSearch(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return;

        order.setStatus(OrderStatus.SEARCHING);
        order.setCourierSearchStartedAt(Instant.now());
        orderRepository.save(order);

        redis.opsForValue().set(ORDER_SEARCH_KEY + orderId, "1",
                Duration.ofMinutes(maxWaitMin + 2));

        log.info("Kuryer qidirish boshlandi: orderId={}", orderId);
        searchWithRadius(orderId, searchRadiusKm, 0);
    }

    /**
     * Ma'lum radiusda kuryer qidirish.
     */
    private void searchWithRadius(UUID orderId, double radius, int skipCount) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.SEARCHING) return;

        // 10 daqiqadan oshgan bo'lsa — vaqt tugagan
        if (order.getCourierSearchStartedAt() != null) {
            long elapsedMin = Duration.between(order.getCourierSearchStartedAt(), Instant.now()).toMinutes();
            if (elapsedMin >= maxWaitMin) {
                handleSearchTimeout(order);
                return;
            }
        }

        List<CourierProfile> candidates = courierProfileRepository.findNearbyCouriers(
                order.getPickupLat(), order.getPickupLng(), radius, skipCount + 5);

        if (candidates.isEmpty() || skipCount >= candidates.size()) {
            // Radius kengaytirish
            if (radius < maxSearchRadiusKm) {
                log.info("Radius kengaytirilmoqda: {} -> {} km, orderId={}", radius, maxSearchRadiusKm, orderId);
                searchWithRadius(orderId, maxSearchRadiusKm, 0);
            } else {
                handleSearchTimeout(order);
            }
            return;
        }

        CourierProfile chosen = candidates.get(skipCount);
        UUID courierId = chosen.getUser().getId();

        // Redis ga taklif yozamiz
        redis.opsForValue().set(PENDING_OFFER_KEY + orderId, courierId.toString(),
                Duration.ofSeconds(acceptTimeoutSec + 5));

        // Telegram push yoki SMS
        notificationService.notifyCourierNewOrder(courierId, order);

        log.info("Kuryer {} ga taklif yuborildi, orderId={}", courierId, orderId);

        // Timeout: kuryerdan javob kutamiz
        final int nextSkip = skipCount + 1;
        final double currentRadius = radius;
        scheduler.schedule(() -> {
            String stillPending = redis.opsForValue().get(PENDING_OFFER_KEY + orderId);
            if (stillPending != null && stillPending.equals(courierId.toString())) {
                // Javob berilmadi — keyingisiga
                redis.delete(PENDING_OFFER_KEY + orderId);
                log.info("Kuryer {} javob bermadi, keyingisi...", courierId);
                searchWithRadius(orderId, currentRadius, nextSkip);
            }
        }, acceptTimeoutSec, TimeUnit.SECONDS);
    }

    /**
     * Kuryer buyurtmani qabul qiladi.
     */
    @Transactional
    public boolean acceptOffer(UUID orderId, UUID courierId) {
        String pendingCourier = redis.opsForValue().get(PENDING_OFFER_KEY + orderId);
        if (!courierId.toString().equals(pendingCourier)) {
            return false; // Taklif muddati o'tgan yoki boshqa kuryer
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.SEARCHING) return false;

        redis.delete(PENDING_OFFER_KEY + orderId);
        redis.delete(ORDER_SEARCH_KEY + orderId);

        var courier = userRepository.findById(courierId).orElse(null);
        if (courier == null) return false;

        order.setStatus(OrderStatus.ASSIGNED);
        order.setCourier(courier);
        order.setAssignedAt(Instant.now());
        orderRepository.save(order);

        notificationService.notifyOrderAssigned(order);

        log.info("Buyurtma {} kuryer {} ga tayinlandi", orderId, courierId);
        return true;
    }

    /**
     * Kuryer buyurtmani rad etadi.
     */
    public void rejectOffer(UUID orderId, UUID courierId) {
        String pendingCourier = redis.opsForValue().get(PENDING_OFFER_KEY + orderId);
        if (courierId.toString().equals(pendingCourier)) {
            redis.delete(PENDING_OFFER_KEY + orderId);
            log.info("Kuryer {} rad etdi, orderId={}", courierId, orderId);
        }
    }

    private void handleSearchTimeout(Order order) {
        redis.delete(ORDER_SEARCH_KEY + order.getId());
        order.setStatus(OrderStatus.FAILED);
        orderRepository.save(order);
        notificationService.notifyCourierNotFound(order);
        log.warn("Buyurtma {} uchun kuryer topilmadi", order.getId());
    }
}
