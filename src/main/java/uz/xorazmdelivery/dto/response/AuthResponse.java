package uz.xorazmdelivery.dto.response;
import lombok.*;
@Data @Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private String role;
    private boolean newUser;
}
