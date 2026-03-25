package uz.xorazmdelivery.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.xorazmdelivery.dto.response.ApiResponse;
import uz.xorazmdelivery.dto.response.CourierLocationResponse;
import uz.xorazmdelivery.security.SecurityUtils;
import uz.xorazmdelivery.service.TrackingService;

import java.util.UUID;

@RestController
@RequestMapping("/v1/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    /**
     * POST /v1/tracking/location
     * Kuryer GPS koordinatini yuboradi (har 30 soniyada)
     */
    @PostMapping("/location")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<ApiResponse<Void>> updateLocation(
            @Valid @RequestBody LocationUpdateRequest request) {
        UUID courierId = SecurityUtils.getCurrentUserId();
        trackingService.updateCourierLocation(courierId, request.getLat(), request.getLng());
        return ResponseEntity.ok(ApiResponse.ok("Joylashuv yangilandi"));
    }

    /**
     * GET /v1/tracking/{orderId}
     * Kuryerning joriy joylashuvini olish
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<CourierLocationResponse>> getCourierLocation(
            @PathVariable UUID orderId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        CourierLocationResponse location = trackingService.getCourierLocation(orderId, userId);
        return ResponseEntity.ok(ApiResponse.ok(location));
    }

    @Data
    static class LocationUpdateRequest {
        @NotNull private Double lat;
        @NotNull private Double lng;
        private Double speed;
        private Double accuracy;
    }
}
