package uz.xorazmdelivery.controller;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.xorazmdelivery.dto.response.ApiResponse;
import uz.xorazmdelivery.enums.PaymentProvider;
import uz.xorazmdelivery.security.SecurityUtils;
import uz.xorazmdelivery.service.PaymentService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** POST /v1/payments/initiate — To'lov boshlash */
    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<Map<String, String>>> initiate(
            @RequestBody InitiateRequest req) {
        var result = paymentService.initiatePayment(
                req.getOrderId(),
                SecurityUtils.getCurrentUserId(),
                req.getProvider()
        );
        return ResponseEntity.ok(ApiResponse.ok("To'lov URL tayyor", result));
    }

    /** POST /v1/payments/callback/click — Click webhook */
    @PostMapping("/callback/click")
    public ResponseEntity<Map<String, Object>> clickCallback(@RequestBody Map<String, Object> payload) {
        paymentService.handleClickCallback(payload);
        return ResponseEntity.ok(Map.of("error", 0, "error_note", "Success"));
    }

    /** POST /v1/payments/callback/payme — Payme JSON-RPC */
    @PostMapping("/callback/payme")
    public ResponseEntity<Map<String, Object>> paymeCallback(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(paymentService.handlePaymeCallback(body));
    }

    @Data
    static class InitiateRequest {
        @NotNull private UUID orderId;
        @NotNull private PaymentProvider provider;
    }
}
