package uz.xorazmdelivery.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;
import uz.xorazmdelivery.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id")
    private User courier;

    @Enumerated(EnumType.STRING)
    @ColumnTransformer(read = "status::varchar", write = "?::order_status")
    @Column(name = "status", nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    // Pickup
    @Column(name = "pickup_address", nullable = false)
    private String pickupAddress;

    @Column(name = "pickup_lat", nullable = false)
    private Double pickupLat;

    @Column(name = "pickup_lng", nullable = false)
    private Double pickupLng;

    // Delivery
    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Column(name = "delivery_lat", nullable = false)
    private Double deliveryLat;

    @Column(name = "delivery_lng", nullable = false)
    private Double deliveryLng;

    @Column(name = "distance_km", precision = 8, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "weight_kg", nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal weightKg = BigDecimal.ONE;

    @Column(name = "is_fragile", nullable = false)
    @Builder.Default
    private boolean fragile = false;

    private String description;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "base_fee", nullable = false)
    @Builder.Default
    private Long baseFee = 0L;

    @Column(name = "total_fee", nullable = false)
    private Long totalFee;

    @Column(name = "is_night_rate", nullable = false)
    @Builder.Default
    private boolean nightRate = false;

    @Column(name = "is_holiday_rate", nullable = false)
    @Builder.Default
    private boolean holidayRate = false;

    @Column(name = "courier_search_started_at")
    private Instant courierSearchStartedAt;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "picked_up_at")
    private Instant pickedUpAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }
}
