package uz.xorazmdelivery.controller;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.xorazmdelivery.dto.response.ApiResponse;
import uz.xorazmdelivery.entity.Review;
import uz.xorazmdelivery.security.SecurityUtils;
import uz.xorazmdelivery.service.ReviewService;

import java.util.UUID;

@RestController
@RequestMapping("/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /** POST /v1/reviews */
    @PostMapping
    public ResponseEntity<ApiResponse<Review>> create(@RequestBody ReviewRequest req) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Review review = reviewService.createReview(userId, req.getOrderId(), req.getRating(), req.getComment());
        return ResponseEntity.ok(ApiResponse.ok("Baholash saqlandi", review));
    }

    @Data
    static class ReviewRequest {
        @NotNull private UUID orderId;
        @Min(1) @Max(5) private short rating;
        @Size(max = 500) private String comment;
    }
}
