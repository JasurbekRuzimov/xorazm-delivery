package uz.xorazmdelivery.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;
import uz.xorazmdelivery.enums.VehicleType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "courier_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourierProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @ColumnTransformer(read = "vehicle_type::varchar", write = "?::vehicle_type")
    @Column(name = "vehicle_type", nullable = false)
    @Builder.Default
    private VehicleType vehicleType = VehicleType.BICYCLE;

    @Column(name = "license_plate", length = 20)
    private String licensePlate;

    @Column(name = "id_card_url")
    private String idCardUrl;

    @Column(name = "license_url")
    private String licenseUrl;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private boolean verified = false;

    @Column(name = "is_online", nullable = false)
    @Builder.Default
    private boolean online = false;

    @Column(nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = new BigDecimal("5.00");

    @Column(name = "total_reviews", nullable = false)
    @Builder.Default
    private Integer totalReviews = 0;

    @Column(name = "total_orders", nullable = false)
    @Builder.Default
    private Integer totalOrders = 0;

    @Column(nullable = false)
    @Builder.Default
    private Long balance = 0L;

    @Column(name = "current_lat")
    private Double currentLat;

    @Column(name = "current_lng")
    private Double currentLng;

    @Column(name = "location_updated_at")
    private Instant locationUpdatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
