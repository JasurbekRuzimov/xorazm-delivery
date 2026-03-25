package uz.xorazmdelivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uz.xorazmdelivery.dto.response.CourierLocationResponse;
import uz.xorazmdelivery.entity.Order;
import uz.xorazmdelivery.enums.OrderStatus;
import uz.xorazmdelivery.repository.CourierProfileRepository;
import uz.xorazmdelivery.repository.OrderRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Real-time tracking arxitekturasi (TZ 5.4):
 * - Kuryer har 30 soniyada GPS koordinat yuboradi
 * - Backend Redis ga yozadi (TTL: 2 daqiqa)
 * - Spring Scheduler har 15 soniyada WebSocket orqali mijozga uzatadi
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingService {

    private static final String LOCATION_KEY = "courier_location:";
    private static final Duration LOCATION_TTL = Duration.ofMinutes(2);

    private final StringRedisTemplate redis;
    private final CourierProfileRepository courierProfileRepository;
    private final OrderRepository orderRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Kuryer GPS koordinatini Redis ga yozadi.
     * Endpoint: POST /v1/tracking/location
     */
    public void updateCourierLocation(UUID courierId, double lat, double lng) {
        String key = LOCATION_KEY + courierId;
        String value = lat + "," + lng + "," + Instant.now().toEpochMilli();
        redis.opsForValue().set(key, value, LOCATION_TTL);

        // DB ga ham yozamiz (tarixiy saqlash)
        courierProfileRepository.updateLocation(courierId, lat, lng, Instant.now());

        log.debug("Kuryer {} joylashuvi yangilandi: {},{}", courierId, lat, lng);
    }

    /**
     * Buyurtma uchun kuryerning joriy joylashuvini qaytaradi.
     */
    public CourierLocationResponse getCourierLocation(UUID orderId, UUID requestingUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new uz.xorazmdelivery.exception.ResourceNotFoundException("Buyurtma", orderId));

        if (order.getCourier() == null) {
            return CourierLocationResponse.builder()
                    .orderId(orderId)
                    .available(false)
                    .message("Kuryer hali tayinlanmagan")
                    .build();
        }

        UUID courierId = order.getCourier().getId();
        String cached = redis.opsForValue().get(LOCATION_KEY + courierId);

        if (cached == null) {
            return CourierLocationResponse.builder()
                    .orderId(orderId)
                    .courierId(courierId)
                    .available(false)
                    .message("Kuryer joylashuvi ma'lum emas (ulanish yo'q)")
                    .build();
        }

        String[] parts = cached.split(",");
        double lat = Double.parseDouble(parts[0]);
        double lng = Double.parseDouble(parts[1]);
        long timestamp = Long.parseLong(parts[2]);

        return CourierLocationResponse.builder()
                .orderId(orderId)
                .courierId(courierId)
                .lat(lat)
                .lng(lng)
                .timestamp(Instant.ofEpochMilli(timestamp))
                .orderStatus(order.getStatus())
                .available(true)
                .build();
    }

    /**
     * Scheduler: har 15 soniyada barcha aktiv buyurtmalarga kuryer joylashuvini broadcast qiladi.
     * WebSocket kanal: /topic/tracking/{orderId}
     */
    @Scheduled(fixedDelay = 15_000)
    public void broadcastLocations() {
        List<Order> activeOrders;
        try {
            activeOrders = orderRepository.findByStatus(OrderStatus.ON_THE_WAY);
            activeOrders.addAll(orderRepository.findByStatus(OrderStatus.PICKED_UP));
        } catch (Exception e) {
            log.warn("broadcastLocations: DB so'rovda xato — {}", e.getMessage());
            return;
        }

        for (Order order : activeOrders) {
            if (order.getCourier() == null) continue;

            UUID courierId = order.getCourier().getId();
            String cached;
            try {
                cached = redis.opsForValue().get(LOCATION_KEY + courierId);
            } catch (Exception e) {
                log.debug("Redis ulanmagan, tracking o'tkazib yuborildi: {}", e.getMessage());
                continue;
            }
            if (cached == null) continue;

            String[] parts = cached.split(",");
            var location = CourierLocationResponse.builder()
                    .orderId(order.getId())
                    .courierId(courierId)
                    .lat(Double.parseDouble(parts[0]))
                    .lng(Double.parseDouble(parts[1]))
                    .timestamp(Instant.ofEpochMilli(Long.parseLong(parts[2])))
                    .orderStatus(order.getStatus())
                    .available(true)
                    .build();

            messagingTemplate.convertAndSend("/topic/tracking/" + order.getId(), location);
        }
    }
}
