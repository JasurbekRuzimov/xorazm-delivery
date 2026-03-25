package uz.xorazmdelivery.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class VerifyOtpRequest {
    @NotBlank @Pattern(regexp = "^\\+998[0-9]{9}$")
    private String phone;
    @NotBlank @Size(min=6, max=6, message = "OTP 6 raqamdan iborat bo'lishi kerak")
    private String otp;
}
