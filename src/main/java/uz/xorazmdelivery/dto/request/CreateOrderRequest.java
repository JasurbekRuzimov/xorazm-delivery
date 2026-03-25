package uz.xorazmdelivery.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOrderRequest {

    @NotBlank(message = "Olib ketish manzili kiritilishi shart")
    private String pickupAddress;

    @NotNull @DecimalMin("-90") @DecimalMax("90")
    private Double pickupLat;

    @NotNull @DecimalMin("-180") @DecimalMax("180")
    private Double pickupLng;

    @NotBlank(message = "Yetkazish manzili kiritilishi shart")
    private String deliveryAddress;

    @NotNull @DecimalMin("-90") @DecimalMax("90")
    private Double deliveryLat;

    @NotNull @DecimalMin("-180") @DecimalMax("180")
    private Double deliveryLng;

    @NotNull @DecimalMin("0.1") @DecimalMax("1000")
    private BigDecimal weightKg;

    private Boolean fragile;

    @Size(max = 500)
    private String description;
}
