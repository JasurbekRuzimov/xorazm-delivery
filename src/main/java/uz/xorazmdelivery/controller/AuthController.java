package uz.xorazmdelivery.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.xorazmdelivery.dto.request.RefreshTokenRequest;
import uz.xorazmdelivery.dto.request.SendOtpRequest;
import uz.xorazmdelivery.dto.request.VerifyOtpRequest;
import uz.xorazmdelivery.dto.response.ApiResponse;
import uz.xorazmdelivery.dto.response.AuthResponse;
import uz.xorazmdelivery.service.AuthService;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /v1/auth/send-otp
     * SMS OTP yuborish
     */
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendOtp(request.getPhone());
        return ResponseEntity.ok(ApiResponse.ok("OTP yuborildi. 5 daqiqa ichida kiriting."));
    }

    /**
     * POST /v1/auth/verify-otp
     * OTP ni tasdiqlash va JWT token olish
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse auth = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.ok(
                auth.isNewUser() ? "Xush kelibsiz! Hisobingiz yaratildi." : "Muvaffaqiyatli kirdingiz.",
                auth));
    }

    /**
     * POST /v1/auth/refresh
     * Access token ni refresh token bilan yangilash
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse auth = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok(auth));
    }

    /**
     * POST /v1/auth/logout
     * Chiqish: token blacklistga qo'shiladi
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            authService.logout(header.substring(7));
        }
        return ResponseEntity.ok(ApiResponse.ok("Chiqish muvaffaqiyatli"));
    }
}
