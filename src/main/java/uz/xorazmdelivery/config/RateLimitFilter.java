package uz.xorazmdelivery.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bucket4j asosida rate limiting filter.
 * - Autentifikatsiyasiz: 20 req/min per IP
 * - Autentifikatsiyalangan: 200 req/min per foydalanuvchi
 */
@Component
@Order(1)
@Slf4j
public class RateLimitFilter implements Filter {

    @Value("${rate-limit.unauthenticated-rpm:20}")
    private int unauthRpm;

    @Value("${rate-limit.authenticated-rpm:200}")
    private int authRpm;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  httpReq = (HttpServletRequest)  req;
        HttpServletResponse httpRes = (HttpServletResponse) res;

        String key    = resolveKey(httpReq);
        boolean isAuth = httpReq.getHeader("Authorization") != null;
        Bucket bucket = buckets.computeIfAbsent(key, k -> isAuth
                ? buildBucket(authRpm)
                : buildBucket(unauthRpm));

        if (bucket.tryConsume(1)) {
            chain.doFilter(req, res);
        } else {
            log.warn("Rate limit exceeded for key: {}", key);
            httpRes.setStatus(429);
            httpRes.setContentType("application/json");
            httpRes.getWriter().write(
                "{\"status\":429,\"error\":\"Too Many Requests\"," +
                "\"message\":\"So'rovlar soni haddan oshdi. Biroz kuting.\"}"
            );
        }
    }

    private String resolveKey(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            // token prefix ni kalit sifatida ishlatamiz (to'liq token kerak emas)
            String token = auth.substring(7);
            return "auth:" + token.substring(0, Math.min(token.length(), 20));
        }
        String ip = req.getHeader("X-Forwarded-For");
        return "ip:" + (ip != null ? ip.split(",")[0].trim() : req.getRemoteAddr());
    }

    private Bucket buildBucket(int rpm) {
        Bandwidth limit = Bandwidth.classic(rpm,
                Refill.greedy(rpm, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
