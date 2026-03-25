package uz.xorazmdelivery.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.xorazmdelivery.repository.SubscriptionRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PriceCalculatorService testlari")
class PriceCalculatorServiceTest {

    @Mock SubscriptionRepository subscriptionRepository;

    @InjectMocks PriceCalculatorService priceService;

    @BeforeEach
    void setUp() throws Exception {
        // Inject @Value fields manually
        setField("baseFee",          5000L);
        setField("perKmFee",         500L);
        setField("perKgFee",         200L);
        setField("minFee",           7000L);
        setField("nightMultiplier",  1.30);
        setField("holidayMultiplier",1.50);
        setField("bulkWeightKg",     10.0);
        setField("bulkDiscount",     0.85);
        setField("premiumDiscount",  0.90);
        setField("nightStartHour",   22);
        setField("nightEndHour",     6);
    }

    private void setField(String name, Object value) throws Exception {
        var field = PriceCalculatorService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(priceService, value);
    }

    @Test
    @DisplayName("Standart narx: 10 km, 1 kg")
    void calculate_standard() {
        // 5000 + 10*500 + 1*200 = 10200
        given(subscriptionRepository.findActiveByUserId(any())).willReturn(Optional.empty());

        var result = priceService.calculatePublic(10.0, 1.0);

        assertThat(result.totalFee()).isEqualTo(10_200L);
        assertThat(result.bulkDiscount()).isFalse();
    }

    @Test
    @DisplayName("Minimal narx qo'llanilishi kerak")
    void calculate_minimumFee() {
        given(subscriptionRepository.findActiveByUserId(any())).willReturn(Optional.empty());

        // 5000 + 1*500 + 0.1*200 = 5520 < 7000 => 7000
        var result = priceService.calculatePublic(1.0, 0.1);

        assertThat(result.totalFee()).isGreaterThanOrEqualTo(7000L);
    }

    @Test
    @DisplayName("Ommaviy yuk uchun chegirma (10+ kg)")
    void calculate_bulkDiscount() {
        given(subscriptionRepository.findActiveByUserId(any())).willReturn(Optional.empty());

        // 5000 + 5*500 + 10*200 = 9500; 9500 * 0.85 = 8075
        var result = priceService.calculatePublic(5.0, 10.0);

        assertThat(result.bulkDiscount()).isTrue();
        assertThat(result.totalFee()).isEqualTo(8_075L);
    }

    @Test
    @DisplayName("Haversine masofa — Urganch-Xiva ~30 km")
    void calculateDistance_urganchToXiva() {
        // Urganch: 41.5333, 60.6333
        // Xiva:    41.3775, 60.3619
        double dist = priceService.calculateDistance(41.5333, 60.6333, 41.3775, 60.3619);

        assertThat(dist).isBetween(25.0, 40.0);
    }

    @Test
    @DisplayName("Bir xil koordinatada masofa = 0")
    void calculateDistance_samePoint() {
        double dist = priceService.calculateDistance(41.5, 60.6, 41.5, 60.6);
        assertThat(dist).isLessThan(0.01);
    }
}
