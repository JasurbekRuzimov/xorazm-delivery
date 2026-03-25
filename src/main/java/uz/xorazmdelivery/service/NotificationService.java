package uz.xorazmdelivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uz.xorazmdelivery.entity.Order;
import uz.xorazmdelivery.entity.User;
import uz.xorazmdelivery.repository.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final EskizSmsService smsService;
    private final UserRepository userRepository;

    @Async
    public void notifyOrderCreated(Order order) {
        String msg = String.format(
            "XorazmDelivery: #%s buyurtmangiz qabul qilindi. Narx: %,d so'm. Kuryer qidirilmoqda...",
            order.getId().toString().substring(0, 8).toUpperCase(),
            order.getTotalFee()
        );
        sendSmsToUser(order.getCustomer(), msg);
    }

    @Async
    public void notifyOrderAssigned(Order order) {
        String msg = String.format(
            "XorazmDelivery: Buyurtmangiz #%s uchun kuryer tayinlandi! Kuzatish: xorazmdelivery.uz/track/%s",
            order.getId().toString().substring(0, 8).toUpperCase(),
            order.getId()
        );
        sendSmsToUser(order.getCustomer(), msg);
    }

    @Async
    public void notifyOrderDelivered(Order order) {
        String msg = String.format(
            "XorazmDelivery: Buyurtmangiz #%s muvaffaqiyatli yetkazildi! Xizmatimizdan foydalanganingiz uchun rahmat.",
            order.getId().toString().substring(0, 8).toUpperCase()
        );
        sendSmsToUser(order.getCustomer(), msg);
    }

    @Async
    public void notifyCourierNewOrder(UUID courierId, Order order) {
        userRepository.findById(courierId).ifPresent(courier -> {
            String msg = String.format(
                "XorazmDelivery: Yangi buyurtma! %s -> %s. Narx: %,d so'm. 60 soniya ichida qabul qiling.",
                shorten(order.getPickupAddress()), shorten(order.getDeliveryAddress()),
                order.getTotalFee()
            );
            sendSmsToUser(courier, msg);
        });
    }

    @Async
    public void notifyCourierNotFound(Order order) {
        String msg = String.format(
            "XorazmDelivery: Afsuski, #%s buyurtmangiz uchun hozircha kuryer topilmadi. Keyinroq urinib ko'ring yoki qo'llab-quvvatlash: +998901234567",
            order.getId().toString().substring(0, 8).toUpperCase()
        );
        sendSmsToUser(order.getCustomer(), msg);
    }

    @Async
    public void notifyBulk(String phone, String message) {
        smsService.send(phone, message);
    }

    private void sendSmsToUser(User user, String message) {
        if (user != null && user.getPhone() != null) {
            smsService.send(user.getPhone(), message);
        }
    }

    private String shorten(String address) {
        if (address == null) return "";
        return address.length() > 30 ? address.substring(0, 27) + "..." : address;
    }
}
