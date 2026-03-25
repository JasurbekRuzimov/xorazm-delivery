package uz.xorazmdelivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendOtpRequest {

    @NotBlank(message = "Telefon raqam bo'sh bo'lishi mumkin emas")
    @Pattern(regexp = "^\\+998[0-9]{9}$", message = "Telefon raqam noto'g'ri formatda (+998XXXXXXXXX)")
    private String phone;
}
