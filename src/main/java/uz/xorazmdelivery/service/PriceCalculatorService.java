package uz.xorazmdelivery.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uz.xorazmdelivery.entity.User;
import uz.xorazmdelivery.enums.SubscriptionPlan;
import uz.xorazmdelivery.repository.SubscriptionRepository;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Narx hisoblash servisi.
 * Formula: Narx = Baza(5000) + Masofa * 500 so'm/km + Og'irlik * 200 so'm/kg
 * Tunda +30%, Bayram +50%, Ommaviy yuk (10+kg) -15%, Premium -10%
 */
@Service
@RequiredArgsConstructor
public class PriceCalculatorService {

    private static final ZoneId TASHKENT_TZ = ZoneId.of("Asia/Tashkent");

    @Value("${delivery.base-fee:5000}")         private long baseFee;
    @Value("${delivery.per-km-fee:500}")        private long perKmFee;
    @Value("${delivery.per-kg-fee:200}")        private long perKgFee;
    @Value("${delivery.min-fee:7000}")          private long minFee;
    @Value("${delivery.night-multiplier:1.30}") private double nightMultiplier;
    @Value("${delivery.holiday-multiplier:1.50}") private double holidayMultiplier;
    @Value("${delivery.bulk-weight-kg:10.0}")   private double bulkWeightKg;
    @Value("${delivery.bulk-discount:0.85}")    private double bulkDiscount;
    @Value("${delivery.premium-discount:0.90}") private double premiumDiscount;
    @Value("${delivery.night-start-hour:22}")   private int nightStartHour;
    @Value("${delivery.night-end-hour:6}")      private int nightEndHour;

    private final SubscriptionRepository subscriptionRepository;

    public PriceResult calculate(double distanceKm, double weightKg, User customer) {
        long price = baseFee
                   + Math.round(distanceKm * perKmFee)
                   + Math.round(weightKg * perKgFee);

        boolean isNight = isNightTime();
        boolean isBulk  = weightKg >= bulkWeightKg;
        boolean isPremium = hasPremium(customer);

        if (isNight)    price = Math.round(price * nightMultiplier);
        if (isBulk)     price = Math.round(price * bulkDiscount);
        if (isPremium)  price = Math.round(price * premiumDiscount);

        price = Math.max(price, minFee);

        return new PriceResult(price, isNight, false, isBulk, isPremium, distanceKm, weightKg);
    }

    /** Foydalanuvchisiz narx kalkulyatsiyasi (umumiy) */
    public PriceResult calculatePublic(double distanceKm, double weightKg) {
        long price = baseFee
                   + Math.round(distanceKm * perKmFee)
                   + Math.round(weightKg * perKgFee);

        boolean isNight = isNightTime();
        boolean isBulk  = weightKg >= bulkWeightKg;

        if (isNight) price = Math.round(price * nightMultiplier);
        if (isBulk)  price = Math.round(price * bulkDiscount);

        price = Math.max(price, minFee);
        return new PriceResult(price, isNight, false, isBulk, false, distanceKm, weightKg);
    }

    /** Haversine formula — ikkita koordinata orasidagi masofani km da hisoblaydi */
    public double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private boolean isNightTime() {
        int hour = ZonedDateTime.now(TASHKENT_TZ).getHour();
        return hour >= nightStartHour || hour < nightEndHour;
    }

    private boolean hasPremium(User user) {
        if (user == null) return false;
        return subscriptionRepository.findActiveByUserId(user.getId())
                .map(s -> s.getPlan() == SubscriptionPlan.PREMIUM || s.getPlan() == SubscriptionPlan.BUSINESS)
                .orElse(false);
    }

    public record PriceResult(
            long totalFee,
            boolean nightRate,
            boolean holidayRate,
            boolean bulkDiscount,
            boolean premiumDiscount,
            double distanceKm,
            double weightKg
    ) {}
}
