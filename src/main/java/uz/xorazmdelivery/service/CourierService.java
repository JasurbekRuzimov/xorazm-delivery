package uz.xorazmdelivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.xorazmdelivery.entity.CourierProfile;
import uz.xorazmdelivery.entity.User;
import uz.xorazmdelivery.exception.BusinessException;
import uz.xorazmdelivery.exception.ResourceNotFoundException;
import uz.xorazmdelivery.repository.CourierProfileRepository;
import uz.xorazmdelivery.repository.OrderRepository;
import uz.xorazmdelivery.repository.UserRepository;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourierService {

    private final CourierProfileRepository courierProfileRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    /**
     * Kuryer profilini olish
     */
    @Transactional(readOnly = true)
    public CourierProfile getProfile(UUID userId) {
        return courierProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kuryer profili topilmadi"));
    }

    /**
     * Online/offline holat o'zgartirish
     */
    @Transactional
    public void setOnlineStatus(UUID userId, boolean online) {
        CourierProfile profile = courierProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kuryer profili topilmadi"));

        if (!profile.isVerified()) {
            throw new BusinessException("Profilingiz hali tasdiqlanmagan. Admin bilan bog'laning.");
        }

        courierProfileRepository.updateOnlineStatus(userId, online);
        log.info("Kuryer {} holati: {}", userId, online ? "ONLINE" : "OFFLINE");
    }

    /**
     * Kuryer ro'yxatga olish (birinchi marta)
     */
    @Transactional
    public CourierProfile register(UUID userId, uz.xorazmdelivery.dto.request.CourierRegisterRequest req) {
        if (courierProfileRepository.existsByUserId(userId)) {
            throw new BusinessException("Kuryer profili allaqachon mavjud");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Foydalanuvchi", userId));
        user.setRole(uz.xorazmdelivery.enums.UserRole.COURIER);
        userRepository.save(user);

        CourierProfile profile = CourierProfile.builder()
                .user(user)
                .vehicleType(req.getVehicleType())
                .licensePlate(req.getLicensePlate())
                .build();

        return courierProfileRepository.save(profile);
    }

    /**
     * Bugungi statistika
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getTodayStats(UUID courierId) {
        long ordersCount = orderRepository.countTodayDeliveredByCourier(courierId);
        long earnings    = orderRepository.sumTodayEarningsByCourier(courierId);
        CourierProfile profile = courierProfileRepository.findByUserId(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Kuryer profili topilmadi"));

        return Map.of(
                "todayOrders",   ordersCount,
                "todayEarnings", earnings,
                "totalOrders",   profile.getTotalOrders(),
                "balance",       profile.getBalance(),
                "rating",        profile.getRating(),
                "isOnline",      profile.isOnline()
        );
    }

    /**
     * To'lov so'rash
     */
    @Transactional
    public void requestPayout(UUID courierId) {
        CourierProfile profile = courierProfileRepository.findByUserId(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Kuryer profili topilmadi"));

        if (profile.getBalance() < 50_000) {
            throw new BusinessException("Minimal to'lov miqdori 50,000 so'm. Joriy balans: "
                    + profile.getBalance() + " so'm");
        }

        // Real implementatsiyada: to'lov tizimiga so'rov yuboriladi
        log.info("Kuryer {} to'lov so'rov yubordi. Balans: {}", courierId, profile.getBalance());
        // profile.setBalance(0L);  // to'lov tasdiqlangandan keyin 0 ga o'tadi
    }
}
