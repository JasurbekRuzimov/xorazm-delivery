package uz.xorazmdelivery.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.xorazmdelivery.dto.response.ApiResponse;
import uz.xorazmdelivery.dto.response.PageResponse;
import uz.xorazmdelivery.entity.User;
import uz.xorazmdelivery.enums.UserRole;
import uz.xorazmdelivery.service.AdminService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /** GET /v1/admin/dashboard */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getDashboard()));
    }

    /** GET /v1/admin/users */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<User>>> users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(adminService.getUsers(pageable))));
    }

    /** PATCH /v1/admin/users/{id}/active */
    @PatchMapping("/users/{id}/active")
    public ResponseEntity<ApiResponse<Void>> setActive(
            @PathVariable UUID id,
            @RequestParam boolean active) {
        adminService.setUserActive(id, active);
        return ResponseEntity.ok(ApiResponse.ok(active ? "Foydalanuvchi faollashtirildi" : "Bloklandi"));
    }

    /** PATCH /v1/admin/couriers/{id}/verify */
    @PatchMapping("/couriers/{id}/verify")
    public ResponseEntity<ApiResponse<Void>> verifyCourier(
            @PathVariable UUID id,
            @RequestParam boolean verified) {
        adminService.verifyCourier(id, verified);
        return ResponseEntity.ok(ApiResponse.ok(verified ? "Kuryer tasdiqlandi" : "Rad etildi"));
    }

    /** POST /v1/admin/broadcast — Ommaviy SMS */
    @PostMapping("/broadcast")
    public ResponseEntity<ApiResponse<Void>> broadcast(@RequestBody BroadcastRequest req) {
        adminService.broadcastSms(req.getMessage(), req.getRole());
        return ResponseEntity.ok(ApiResponse.ok("SMS yuborilmoqda..."));
    }

    @Data
    static class BroadcastRequest {
        private String message;
        private UserRole role; // null = hammaga
    }
}
