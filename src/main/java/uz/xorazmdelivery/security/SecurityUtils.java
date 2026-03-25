package uz.xorazmdelivery.security;

import org.springframework.security.core.context.SecurityContextHolder;
import uz.xorazmdelivery.exception.UnauthorizedException;

import java.util.UUID;

public final class SecurityUtils {
    private SecurityUtils() {}

    public static UUID getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Foydalanuvchi autentifikatsiya qilinmagan");
        }
        return (UUID) auth.getPrincipal();
    }
}
