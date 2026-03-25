package uz.xorazmdelivery.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * JWT utility — RS256 asimmetrik imzolash.
 * MVP uchun: har start da yangi keypair yaratiladi.
 * Production: .pem fayllardan o'qilishi kerak.
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.access-token-expiry:900}")
    private long accessTokenExpirySeconds;

    @Value("${jwt.refresh-token-expiry:2592000}")
    private long refreshTokenExpirySeconds;

    private KeyPair keyPair;

    @PostConstruct
    public void init() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            this.keyPair = gen.generateKeyPair();
            log.info("JWT RS256 keypair initialized");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize JWT keypair", e);
        }
    }

    public String generateAccessToken(UUID userId, String role) {
        return buildToken(userId.toString(), "access", role, accessTokenExpirySeconds * 1000);
    }

    public String generateRefreshToken(UUID userId) {
        return buildToken(userId.toString(), "refresh", null, refreshTokenExpirySeconds * 1000);
    }

    private String buildToken(String subject, String type, String role, long expiryMs) {
        var now = new Date();
        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMs))
                .claim("type", type)
                .id(UUID.randomUUID().toString());

        if (role != null) builder.claim("role", role);

        return builder
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(keyPair.getPublic())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return "access".equals(claims.get("type", String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("type", String.class));
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public String extractRole(Claims claims) {
        return claims.get("role", String.class);
    }

    public long getRefreshTokenExpirySeconds() {
        return refreshTokenExpirySeconds;
    }
}
