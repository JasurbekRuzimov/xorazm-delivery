package uz.xorazmdelivery.controller;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.xorazmdelivery.dto.response.ApiResponse;
import uz.xorazmdelivery.entity.User;
import uz.xorazmdelivery.exception.ResourceNotFoundException;
import uz.xorazmdelivery.repository.UserRepository;
import uz.xorazmdelivery.security.SecurityUtils;

import java.util.UUID;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /** GET /v1/users/me — O'z profili */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getMe() {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Foydalanuvchi", userId));
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    /** PATCH /v1/users/me — Profil yangilash */
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<User>> updateMe(@RequestBody UpdateProfileRequest req) {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Foydalanuvchi", userId));

        if (req.getFullName() != null) user.setFullName(req.getFullName());
        if (req.getLang()     != null) user.setLang(req.getLang());

        user = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("Profil yangilandi", user));
    }

    /** DELETE /v1/users/me — Hisobni o'chirish (GDPR) */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMe() {
        UUID userId = SecurityUtils.getCurrentUserId();
        userRepository.deleteById(userId);   // soft delete (deleted_at set)
        return ResponseEntity.ok(ApiResponse.ok("Hisobingiz o'chirildi. 30 kun ichida barcha ma'lumotlar yo'q qilinadi."));
    }

    @Data
    static class UpdateProfileRequest {
        @Size(max = 100) private String fullName;
        @Size(min = 2, max = 5) private String lang;
    }
}
