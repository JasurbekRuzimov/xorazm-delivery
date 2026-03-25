package uz.xorazmdelivery.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.xorazmdelivery.entity.Order;
import uz.xorazmdelivery.entity.Review;
import uz.xorazmdelivery.enums.OrderStatus;
import uz.xorazmdelivery.exception.BusinessException;
import uz.xorazmdelivery.exception.ResourceNotFoundException;
import uz.xorazmdelivery.repository.OrderRepository;
import uz.xorazmdelivery.repository.ReviewRepository;
import uz.xorazmdelivery.repository.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository  reviewRepository;
    private final OrderRepository   orderRepository;
    private final UserRepository    userRepository;

    @Transactional
    public Review createReview(UUID reviewerId, UUID orderId, short rating, String comment) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyurtma", orderId));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException("Faqat yetkazilgan buyurtmani baholash mumkin");
        }
        if (!order.getCustomer().getId().equals(reviewerId)) {
            throw new BusinessException("Bu buyurtmani baholash huquqingiz yo'q");
        }
        if (order.getCourier() == null) {
            throw new BusinessException("Buyurtmada kuryer topilmadi");
        }

        boolean exists = reviewRepository.existsByOrderId(orderId);
        if (exists) throw new BusinessException("Bu buyurtma allaqachon baholangan");

        var reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Foydalanuvchi", reviewerId));

        Review review = Review.builder()
                .order(order)
                .reviewer(reviewer)
                .target(order.getCourier())
                .rating(rating)
                .comment(comment)
                .build();

        return reviewRepository.save(review);
    }
}
