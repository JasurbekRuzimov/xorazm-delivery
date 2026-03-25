package uz.xorazmdelivery.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.xorazmdelivery.dto.response.ApiResponse;
import uz.xorazmdelivery.service.PriceCalculatorService;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/v1/price")
@RequiredArgsConstructor
public class PriceController {

    private final PriceCalculatorService priceService;

    /**
     * POST /v1/price/calculate
     * Narx kalkulyatsiya — autentifikatsiyasiz ishlaydi
     */
    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculate(
            @Valid @RequestBody PriceRequest req) {

        double distance;
        if (req.getPickupLat() != null && req.getPickupLng() != null
                && req.getDeliveryLat() != null && req.getDeliveryLng() != null) {
            distance = priceService.calculateDistance(
                    req.getPickupLat(), req.getPickupLng(),
                    req.getDeliveryLat(), req.getDeliveryLng());
        } else if (req.getDistanceKm() != null) {
            distance = req.getDistanceKm();
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("Koordinatalar yoki masofa kiritilishi shart")
                            .build());
        }

        double weight = req.getWeightKg() != null ? req.getWeightKg() : 1.0;
        var result = priceService.calculatePublic(distance, weight);

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "totalFee",       result.totalFee(),
                "distanceKm",     Math.round(distance * 100.0) / 100.0,
                "weightKg",       weight,
                "isNightRate",    result.nightRate(),
                "isBulkDiscount", result.bulkDiscount(),
                "currency",       "UZS",
                "breakdown", Map.of(
                        "baseFee",    5000,
                        "distanceFee", Math.round(distance * 500),
                        "weightFee",   Math.round(weight * 200)
                )
        )));
    }

    @Data
    static class PriceRequest {
        private Double pickupLat;
        private Double pickupLng;
        private Double deliveryLat;
        private Double deliveryLng;
        private Double distanceKm;
        @Positive private Double weightKg;
    }
}
