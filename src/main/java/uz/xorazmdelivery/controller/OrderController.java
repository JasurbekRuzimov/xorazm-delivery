package uz.xorazmdelivery.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.xorazmdelivery.dto.request.CreateOrderRequest;
import uz.xorazmdelivery.dto.request.UpdateOrderStatusRequest;
import uz.xorazmdelivery.dto.response.ApiResponse;
import uz.xorazmdelivery.dto.response.PageResponse;
import uz.xorazmdelivery.entity.Order;
import uz.xorazmdelivery.security.SecurityUtils;
import uz.xorazmdelivery.service.DeliveryAssignmentService;
import uz.xorazmdelivery.service.OrderService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final DeliveryAssignmentService assignmentService;

    /** POST /v1/orders — Yangi buyurtma */
    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER','FARMER')")
    public ResponseEntity<ApiResponse<Order>> create(@Valid @RequestBody CreateOrderRequest req) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Order order = orderService.createOrder(userId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Buyurtma yaratildi", order));
    }

    /** GET /v1/orders/{id} — Buyurtma tafsiloti */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> getById(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Order order = orderService.getOrderById(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(order));
    }

    /** GET /v1/orders/my — O'z buyurtmalar tarixi */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('CUSTOMER','FARMER')")
    public ResponseEntity<ApiResponse<PageResponse<Order>>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var result = orderService.getMyOrders(userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    /** PATCH /v1/orders/{id}/status — Kuryer holat yangilash */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<ApiResponse<Order>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest req) {
        UUID courierId = SecurityUtils.getCurrentUserId();
        Order order = orderService.updateStatus(id, courierId, req);
        return ResponseEntity.ok(ApiResponse.ok("Holat yangilandi", order));
    }

    /** DELETE /v1/orders/{id}/cancel — Bekor qilish */
    @DeleteMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER','FARMER')")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        String reason = body != null ? body.getOrDefault("reason", "Foydalanuvchi tomonidan bekor qilindi") : "";
        orderService.cancelOrder(id, userId, reason);
        return ResponseEntity.ok(ApiResponse.ok("Buyurtma bekor qilindi"));
    }

    /** POST /v1/orders/{id}/accept — Kuryer qabul qiladi */
    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<ApiResponse<Void>> acceptOffer(@PathVariable UUID id) {
        UUID courierId = SecurityUtils.getCurrentUserId();
        boolean accepted = assignmentService.acceptOffer(id, courierId);
        if (!accepted) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Taklif muddati o'tgan yoki boshqa kuryer qabul qildi")
                            .build());
        }
        return ResponseEntity.ok(ApiResponse.ok("Buyurtma qabul qilindi!"));
    }

    /** POST /v1/orders/{id}/reject — Kuryer rad etadi */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<ApiResponse<Void>> rejectOffer(@PathVariable UUID id) {
        UUID courierId = SecurityUtils.getCurrentUserId();
        assignmentService.rejectOffer(id, courierId);
        return ResponseEntity.ok(ApiResponse.ok("Rad etildi"));
    }
}
