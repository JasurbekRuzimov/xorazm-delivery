package uz.xorazmdelivery.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.xorazmdelivery.dto.request.CourierRegisterRequest;
import uz.xorazmdelivery.dto.response.ApiResponse;
import uz.xorazmdelivery.entity.CourierProfile;
import uz.xorazmdelivery.security.SecurityUtils;
import uz.xorazmdelivery.service.CourierService;

import java.util.Map;

@RestController
@RequestMapping("/v1/couriers")
@RequiredArgsConstructor
public class CourierController {

    private final CourierService courierService;

    /** POST /v1/couriers/register — Kuryer ro'yxatga olish */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<CourierProfile>> register(
            @Valid @RequestBody CourierRegisterRequest req) {
        var profile = courierService.register(SecurityUtils.getCurrentUserId(), req);
        return ResponseEntity.ok(ApiResponse.ok("Kuryer profili yaratildi. Admin tasdiqlashini kuting.", profile));
    }

    /** GET /v1/couriers/me — Kuryer profili */
    @GetMapping("/me")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<ApiResponse<CourierProfile>> getMyProfile() {
        var profile = courierService.getProfile(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    /** PATCH /v1/couriers/me/status — Online/offline */
    @PatchMapping("/me/status")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<ApiResponse<Void>> setStatus(@RequestParam boolean online) {
        courierService.setOnlineStatus(SecurityUtils.getCurrentUserId(), online);
        return ResponseEntity.ok(ApiResponse.ok(online ? "Siz online bo'ldingiz" : "Siz offline bo'ldingiz"));
    }

    /** GET /v1/couriers/me/stats — Bugungi statistika */
    @GetMapping("/me/stats")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        var stats = courierService.getTodayStats(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    /** POST /v1/couriers/me/payout — To'lov so'rash */
    @PostMapping("/me/payout")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<ApiResponse<Void>> requestPayout() {
        courierService.requestPayout(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok("To'lov so'rovi qabul qilindi. 1 ish kunida o'tkaziladi."));
    }
}
