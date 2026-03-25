package uz.xorazmdelivery.dto.response;

import lombok.*;
import uz.xorazmdelivery.enums.OrderStatus;

import java.time.Instant;
import java.util.UUID;

@Data @Builder
public class CourierLocationResponse {
    private UUID orderId;
    private UUID courierId;
    private Double lat;
    private Double lng;
    private Instant timestamp;
    private OrderStatus orderStatus;
    private boolean available;
    private String message;
}
