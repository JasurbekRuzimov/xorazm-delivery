package uz.xorazmdelivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.xorazmdelivery.dto.request.VerifyOtpRequest;
import uz.xorazmdelivery.dto.response.AuthResponse;
import uz.xorazmdelivery.entity.User;
import uz.xorazmdelivery.exception.BusinessException;
import uz.xorazmdelivery.exception.TooManyRequestsException;
import uz.xorazmdelivery.repository.UserRepository;
import uz.xorazmdelivery.security.JwtTokenProvider;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String OTP_KEY      = "otp:";
    private static final String OTP_ATTEMPT  = "otp:attempts:";
    private static final String BLACKLIST_KEY = "blacklist:";

    private static final Duration OTP_TTL   = Duration.ofMinutes(5);
    private static final int MAX_ATTEMPTS   = 3;

    private final StringRedisTemplate redis;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtProvider;
    private final EskizSmsService smsService;

    // Redis o'chiq bo'lganda OTP ni xotirada saqlaymiz (faqat dev uchun)
    private final Map<String, String>  localOtpStore  = new ConcurrentHashMap<>();
    private final Map<String, Instant> localOtpExpiry = new ConcurrentHashMap<>();

    // ─── OTP saqlash (Redis yoki xotira) ─────────────────────────────

    private void saveOtp(String phone, String otp) {
        try {
            redis.opsForValue().set(OTP_KEY + phone, otp, OTP_TTL);
            return;
        } catch (Exception e) {
            log.warn("[DEV] Redis yo'q, OTP xotirada saqlanadi");
        }
        localOtpStore.put(phone, otp);
        localOtpExpiry.put(phone, Instant.now().plus(OTP_TTL));
    }

    private String getOtp(String phone) {
        try {
            return redis.opsForValue().get(OTP_KEY + phone);
        } catch (Exception e) {
            // Redis yo'q — xotiradan o'qiymiz
        }
        Instant expiry = localOtpExpiry.get(phone);
        if (expiry != null && Instant.now().isAfter(expiry)) {
            localOtpStore.remove(phone);
            localOtpExpiry.remove(phone);
            return null;
        }
        return localOtpStore.get(phone);
    }

    private void deleteOtp(String phone) {
        try {
            redis.delete(OTP_KEY + phone);
            redis.delete(OTP_ATTEMPT + phone);
        } catch (Exception ignored) {}
        localOtpStore.remove(phone);
        localOtpExpiry.remove(phone);
    }

    // ─── Public API ──────────────────────────────────────────────────

    public void sendOtp(String phone) {
        try {
            String attemptsStr = redis.opsForValue().get(OTP_ATTEMPT + phone);
            int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;
            if (attempts >= MAX_ATTEMPTS) {
                throw new TooManyRequestsException(
                    "Telefon raqamingiz vaqtinchalik bloklandi. 30 daqiqadan so'ng urinib ko'ring.");
            }
            redis.opsForValue().increment(OTP_ATTEMPT + phone);
            if (attempts == 0) redis.expire(OTP_ATTEMPT + phone, Duration.ofHours(1));
        } catch (TooManyRequestsException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[DEV] Redis yo'q, rate limit o'tkazib yuborildi");
        }

        String otp = generateOtp();
        saveOtp(phone, otp);

        try {
            String text = "XorazmDelivery tasdiqlash kodi: " + otp + ". 5 daqiqa amal qiladi.";
            smsService.send(phone, text);
        } catch (Exception e) {
            log.warn("[DEV] SMS yuborilmadi: {}", e.getMessage());
        }

        // DEV: OTP ni IntelliJ console da ko'rsatamiz
        log.info("╔══════════════════════════════════╗");
        log.info("║  DEV OTP │ {}  →  {}  ║", phone, otp);
        log.info("╚══════════════════════════════════╝");
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        String storedOtp = getOtp(request.getPhone());

        if (storedOtp == null) {
            throw new BusinessException("OTP muddati o'tgan yoki yuborilmagan");
        }
        if (!storedOtp.equals(request.getOtp())) {
            throw new BusinessException("OTP noto'g'ri");
        }

        deleteOtp(request.getPhone());

        boolean isNewUser = !userRepository.existsByPhone(request.getPhone());
        User user = userRepository.findByPhone(request.getPhone())
                .orElseGet(() -> userRepository.save(User.builder()
                        .phone(request.getPhone())
                        .build()));

        if (!user.isActive()) {
            throw new BusinessException("Hisobingiz bloklangan. Qo'llab-quvvatlash bilan bog'laning.");
        }

        if (!user.isVerified()) {
            user.setVerified(true);
            userRepository.save(user);
        }

        return buildAuthResponse(user, isNewUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        var claims = jwtProvider.validateToken(refreshToken);

        if (!jwtProvider.isRefreshToken(claims)) {
            throw new BusinessException("Noto'g'ri token turi");
        }

        UUID userId = jwtProvider.extractUserId(claims);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Foydalanuvchi topilmadi"));

        if (!user.isActive()) {
            throw new BusinessException("Hisobingiz bloklangan");
        }

        try {
            redis.opsForValue().set(BLACKLIST_KEY + refreshToken, "1",
                    Duration.ofSeconds(jwtProvider.getRefreshTokenExpirySeconds()));
        } catch (Exception e) {
            log.warn("[DEV] Redis yo'q, token blacklist o'tkazib yuborildi");
        }

        return buildAuthResponse(user, false);
    }

    public void logout(String accessToken) {
        try {
            var claims = jwtProvider.validateToken(accessToken);
            long remainingMs = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (remainingMs > 0) {
                redis.opsForValue().set(BLACKLIST_KEY + accessToken, "1",
                        Duration.ofMillis(remainingMs));
            }
        } catch (Exception e) {
            log.warn("[DEV] Logout: {}", e.getMessage());
        }
    }

    private AuthResponse buildAuthResponse(User user, boolean newUser) {
        String role = user.getRole().name();
        String accessToken  = jwtProvider.generateAccessToken(user.getId(), role);
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(900L)
                .role(role)
                .newUser(newUser)
                .build();
    }

    private String generateOtp() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }
}
