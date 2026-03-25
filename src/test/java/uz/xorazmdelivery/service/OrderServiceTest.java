package uz.xorazmdelivery.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import uz.xorazmdelivery.dto.request.CreateOrderRequest;
import uz.xorazmdelivery.dto.request.UpdateOrderStatusRequest;
import uz.xorazmdelivery.entity.Order;
import uz.xorazmdelivery.entity.User;
import uz.xorazmdelivery.enums.OrderStatus;
import uz.xorazmdelivery.exception.BusinessException;
import uz.xorazmdelivery.exception.ForbiddenException;
import uz.xorazmdelivery.repository.OrderRepository;
import uz.xorazmdelivery.repository.UserRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService testlari")
class OrderServiceTest {

    @Mock OrderRepository            orderRepository;
    @Mock UserRepository             userRepository;
    @Mock PriceCalculatorService     priceService;
    @Mock DeliveryAssignmentService  assignmentService;
    @Mock NotificationService        notificationService;
    @Mock StringRedisTemplate        redis;

    @InjectMocks OrderService orderService;

    private User buildUser(UUID id) {
        return User.builder().id(id).phone("+998901234567").active(true).verified(true).build();
    }

    private Order buildOrder(UUID id, UUID customerId, OrderStatus status) {
        var customer = buildUser(customerId);
        return Order.builder()
                .id(id)
                .customer(customer)
                .status(status)
                .pickupAddress("Urganch, Mustaqillik 1")
                .pickupLat(41.53).pickupLng(60.63)
                .deliveryAddress("Xiva, Polvon Qori 5")
                .deliveryLat(41.37).deliveryLng(60.36)
                .totalFee(12000L)
                .weightKg(BigDecimal.ONE)
                .build();
    }

    @Test
    @DisplayName("Yangi buyurtma muvaffaqiyatli yaratiladi")
    void createOrder_success() {
        UUID customerId = UUID.randomUUID();
        var customer = buildUser(customerId);

        var req = new CreateOrderRequest();
        req.setPickupAddress("Urganch");
        req.setPickupLat(41.53); req.setPickupLng(60.63);
        req.setDeliveryAddress("Xiva");
        req.setDeliveryLat(41.37); req.setDeliveryLng(60.36);
        req.setWeightKg(BigDecimal.ONE);

        given(userRepository.findById(customerId)).willReturn(Optional.of(customer));
        given(priceService.calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(28.5);
        given(priceService.calculate(anyDouble(), anyDouble(), any()))
                .willReturn(new PriceCalculatorService.PriceResult(19_250L, false, false, false, false, 28.5, 1.0));
        given(orderRepository.save(any())).willAnswer(inv -> {
            Order o = inv.getArgument(0);
            o = Order.builder()
                    .id(UUID.randomUUID())
                    .customer(customer)
                    .status(OrderStatus.PENDING)
                    .totalFee(19_250L)
                    .pickupAddress(req.getPickupAddress())
                    .pickupLat(req.getPickupLat()).pickupLng(req.getPickupLng())
                    .deliveryAddress(req.getDeliveryAddress())
                    .deliveryLat(req.getDeliveryLat()).deliveryLng(req.getDeliveryLng())
                    .weightKg(BigDecimal.ONE)
                    .build();
            return o;
        });

        var order = orderService.createOrder(customerId, req);

        assertThat(order).isNotNull();
        assertThat(order.getTotalFee()).isEqualTo(19_250L);
        then(assignmentService).should().startSearch(any());
        then(notificationService).should().notifyOrderCreated(any());
    }

    @Test
    @DisplayName("Noto'g'ri holat o'tishi BusinessException chiqaradi")
    void updateStatus_invalidTransition() {
        UUID orderId   = UUID.randomUUID();
        UUID courierId = UUID.randomUUID();
        var courier    = buildUser(courierId);
        var order      = buildOrder(orderId, UUID.randomUUID(), OrderStatus.PENDING);
        order.setCourier(courier);

        var req = new UpdateOrderStatusRequest();
        req.setStatus(OrderStatus.DELIVERED);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(orderId, courierId, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Noto'g'ri holat");
    }

    @Test
    @DisplayName("Boshqa kuryer buyurtmani o'zgartira olmaydi")
    void updateStatus_wrongCourier() {
        UUID orderId        = UUID.randomUUID();
        UUID realCourierId  = UUID.randomUUID();
        UUID fakeCourierId  = UUID.randomUUID();
        var realCourier     = buildUser(realCourierId);
        var order           = buildOrder(orderId, UUID.randomUUID(), OrderStatus.ASSIGNED);
        order.setCourier(realCourier);

        var req = new UpdateOrderStatusRequest();
        req.setStatus(OrderStatus.PICKED_UP);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(orderId, fakeCourierId, req))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Yetkazilayotgan buyurtmani bekor qilib bo'lmaydi")
    void cancelOrder_activeDelivery() {
        UUID orderId    = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        var order       = buildOrder(orderId, customerId, OrderStatus.ON_THE_WAY);

        given(orderRepository.findByIdAndCustomerId(orderId, customerId))
                .willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(orderId, customerId, "Sabab"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("bekor qilib bo'lmaydi");
    }
}
