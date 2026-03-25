package uz.xorazmdelivery.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import uz.xorazmdelivery.enums.OrderStatus;

@Data
public class UpdateOrderStatusRequest {

    @NotNull(message = "Holat kiritilishi shart")
    private OrderStatus status;
}
