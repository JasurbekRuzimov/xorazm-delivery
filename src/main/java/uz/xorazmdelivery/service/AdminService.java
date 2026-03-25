package uz.xorazmdelivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.xorazmdelivery.entity.User;
import uz.xorazmdelivery.enums.OrderStatus;
import uz.xorazmdelivery.enums.UserRole;
import uz.xorazmdelivery.exception.ResourceNotFoundException;
import uz.xorazmdelivery.repository.*;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository             userRepository;
    private final OrderRepository            orderRepository;
    private final CourierProfileRepository   courierProfileRepository;
    private final SubscriptionRepository     subscriptionRepository;
    private final NotificationService        notificationService;

    /** Dashboard umumiy statistika */
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard() {
        long totalUsers     = userRepository.count();
        long totalCouriers  = userRepository.findAllByRole(UserRole.COURIER).size();
        long activeCouriers = courierProfileRepository.findNearbyCouriers(0,0,99999,999).size();
        long premiumSubs    = subscriptionRepository.countActivePremium();
        long businessSubs   = subscriptionRepository.countActiveBusiness();

        long pendingOrders   = orderRepository.findByStatus(OrderStatus.SEARCHING).size();
        long activeOrders    = orderRepository.findByStatus(OrderStatus.ON_THE_WAY).size()
                             + orderRepository.findByStatus(OrderStatus.PICKED_UP).size();

        return Map.of(
                "totalUsers",     totalUsers,
                "totalCouriers",  totalCouriers,
                "activeCouriers", activeCouriers,
                "premiumSubs",    premiumSubs,
                "businessSubs",   businessSubs,
                "pendingOrders",  pendingOrders,
                "activeOrders",   activeOrders
        );
    }

    /** Foydalanuvchilar ro'yxati */
    @Transactional(readOnly = true)
    public Page<User> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /** Foydalanuvchini bloklash/ochish */
    @Transactional
    public void setUserActive(UUID userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Foydalanuvchi", userId));
        user.setActive(active);
        userRepository.save(user);
        log.info("Admin: foydalanuvchi {} holati -> {}", userId, active ? "faol" : "bloklangan");
    }

    /** Kuryer profilini tasdiqlash */
    @Transactional
    public void verifyCourier(UUID courierId, boolean verified) {
        var profile = courierProfileRepository.findByUserId(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Kuryer profili topilmadi"));
        profile.setVerified(verified);
        courierProfileRepository.save(profile);
        log.info("Admin: kuryer {} -> {}", courierId, verified ? "tasdiqlandi" : "rad etildi");
    }

    /** Barcha yoki tanlangan foydalanuvchilarga SMS yuborish */
    @Transactional(readOnly = true)
    public void broadcastSms(String message, UserRole role) {
        var users = (role != null)
                ? userRepository.findActiveByRole(role)
                : userRepository.findAll();

        users.forEach(u -> notificationService.notifyBulk(u.getPhone(), message));
        log.info("Admin: {} ta foydalanuvchiga SMS yuborildi", users.size());
    }
}
