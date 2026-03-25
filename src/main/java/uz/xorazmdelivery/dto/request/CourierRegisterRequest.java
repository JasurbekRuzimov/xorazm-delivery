package uz.xorazmdelivery.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import uz.xorazmdelivery.enums.VehicleType;

@Data
public class CourierRegisterRequest {
    @NotNull
    private VehicleType vehicleType;
    private String licensePlate;
}
