package uz.xorazmdelivery.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import uz.xorazmdelivery.dto.request.VerifyOtpRequest;
import uz.xorazmdelivery.entity.User;
import uz.xorazmdelivery.exception.BusinessException;
import uz.xorazmdelivery.exception.TooManyRequestsException;
import uz.xorazmdelivery.repository.UserRepository;
import uz.xorazmdelivery.security.JwtTokenProvider;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService testlari")
class AuthServiceTest {

    @Mock StringRedisTemplate   redis;
    @Mock ValueOperations<String,String> valueOps;
    @Mock UserRepository        userRepository;
    @Mock JwtTokenProvider      jwtProvider;
    @Mock EskizSmsService       smsService;

    @InjectMocks AuthService authService;

    private static final String PHONE = "+998901234567";

    @BeforeEach
    void setUp() {
        given(redis.opsForValue()).willReturn(valueOps);
    }

    @Test
    @DisplayName("OTP muvaffaqiyatli yuborilishi kerak")
    void sendOtp_success() {
        given(valueOps.get("otp:attempts:" + PHONE)).willReturn(null);
        willDoNothing().given(smsService).send(anyString(), anyString());

        assertThatNoException().isThrownBy(() -> authService.sendOtp(PHONE));

        then(smsService).should().send(eq(PHONE), contains("XorazmDelivery"));
    }

    @Test
    @DisplayName("3 dan ortiq urinishda TooManyRequestsException")
    void sendOtp_rateLimitExceeded() {
        given(valueOps.get("otp:attempts:" + PHONE)).willReturn("3");

        assertThatThrownBy(() -> authService.sendOtp(PHONE))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("bloklandi");
    }

    @Test
    @DisplayName("To'g'ri OTP bilan muvaffaqiyatli login")
    void verifyOtp_success_existingUser() {
        var request = new VerifyOtpRequest();
        request.setPhone(PHONE);
        request.setOtp("123456");

        given(valueOps.get("otp:" + PHONE)).willReturn("123456");

        User user = User.builder()
                .id(UUID.randomUUID())
                .phone(PHONE)
                .verified(true)
                .active(true)
                .build();

        given(userRepository.existsByPhone(PHONE)).willReturn(true);
        given(userRepository.findByPhone(PHONE)).willReturn(Optional.of(user));
        given(jwtProvider.generateAccessToken(any(), any())).willReturn("access-token");
        given(jwtProvider.generateRefreshToken(any())).willReturn("refresh-token");
        given(jwtProvider.getRefreshTokenExpirySeconds()).willReturn(2592000L);

        var result = authService.verifyOtp(request);

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.isNewUser()).isFalse();
    }

    @Test
    @DisplayName("Noto'g'ri OTP bilan BusinessException")
    void verifyOtp_wrongOtp() {
        var request = new VerifyOtpRequest();
        request.setPhone(PHONE);
        request.setOtp("000000");

        given(valueOps.get("otp:" + PHONE)).willReturn("123456");

        assertThatThrownBy(() -> authService.verifyOtp(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("noto'g'ri");
    }

    @Test
    @DisplayName("Muddati o'tgan OTP bilan BusinessException")
    void verifyOtp_expiredOtp() {
        var request = new VerifyOtpRequest();
        request.setPhone(PHONE);
        request.setOtp("123456");

        given(valueOps.get("otp:" + PHONE)).willReturn(null);

        assertThatThrownBy(() -> authService.verifyOtp(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("muddati");
    }

    @Test
    @DisplayName("Bloklangan foydalanuvchi login qila olmaydi")
    void verifyOtp_blockedUser() {
        var request = new VerifyOtpRequest();
        request.setPhone(PHONE);
        request.setOtp("123456");

        given(valueOps.get("otp:" + PHONE)).willReturn("123456");

        User blockedUser = User.builder()
                .id(UUID.randomUUID())
                .phone(PHONE)
                .active(false)
                .build();
        given(userRepository.existsByPhone(PHONE)).willReturn(true);
        given(userRepository.findByPhone(PHONE)).willReturn(Optional.of(blockedUser));

        assertThatThrownBy(() -> authService.verifyOtp(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("bloklangan");
    }
}
