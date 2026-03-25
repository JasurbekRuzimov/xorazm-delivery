package uz.xorazmdelivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import uz.xorazmdelivery.entity.Order;
import uz.xorazmdelivery.entity.Payment;
import uz.xorazmdelivery.enums.PaymentProvider;
import uz.xorazmdelivery.enums.PaymentStatus;
import uz.xorazmdelivery.exception.BusinessException;
import uz.xorazmdelivery.exception.ResourceNotFoundException;
import uz.xorazmdelivery.repository.OrderRepository;
import uz.xorazmdelivery.repository.PaymentRepository;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${click.merchant-id}") private String clickMerchantId;
    @Value("${click.service-id}")  private String clickServiceId;
    @Value("${click.secret-key}")  private String clickSecretKey;

    @Value("${payme.merchant-id}") private String paymeMerchantId;

    /**
     * To'lovni boshlash — Click yoki Payme uchun to'lov URL qaytaradi
     */
    @Transactional
    public Map<String, String> initiatePayment(UUID orderId, UUID customerId, PaymentProvider provider) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyurtma", orderId));

        if (!order.getCustomer().getId().equals(customerId)) {
            throw new BusinessException("Bu buyurtma sizga tegishli emas");
        }

        boolean alreadyPaid = paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.PAID);
        if (alreadyPaid) {
            throw new BusinessException("Bu buyurtma allaqachon to'langan");
        }

        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getTotalFee())
                .provider(provider)
                .status(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        String paymentUrl = switch (provider) {
            case CLICK -> buildClickUrl(payment);
            case PAYME -> buildPaymeUrl(payment);
            case CASH  -> null;
        };

        log.info("To'lov boshlandi: orderId={}, provider={}, amount={}", orderId, provider, order.getTotalFee());

        return paymentUrl != null
                ? Map.of("paymentId", payment.getId().toString(), "paymentUrl", paymentUrl)
                : Map.of("paymentId", payment.getId().toString(), "method", "CASH");
    }

    /**
     * Click callback — to'lov tasdiqlash/bekor qilish
     */
    @Transactional
    public void handleClickCallback(Map<String, Object> payload) {
        String merchantTransId = (String) payload.get("merchant_trans_id");
        Integer errorCode      = (Integer) payload.get("error");

        UUID paymentId = UUID.fromString(merchantTransId);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("To'lov", paymentId));

        if (errorCode == 0) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(Instant.now());
            payment.setTransactionId(String.valueOf(payload.get("click_trans_id")));
            log.info("Click to'lov muvaffaqiyatli: {}", paymentId);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            log.warn("Click to'lov muvaffaqiyatsiz: {}, error={}", paymentId, errorCode);
        }
        paymentRepository.save(payment);
    }

    /**
     * Payme callback (JSON-RPC)
     */
    @Transactional
    public Map<String, Object> handlePaymeCallback(Map<String, Object> body) {
        String method = (String) body.get("method");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) body.get("params");

        return switch (method) {
            case "CheckPerformTransaction" -> checkPerformTransaction(params);
            case "CreateTransaction"       -> createTransaction(params);
            case "PerformTransaction"      -> performTransaction(params);
            case "CancelTransaction"       -> cancelTransaction(params);
            default -> Map.of("error", Map.of("code", -32601, "message", "Method not found"));
        };
    }

    // ─── private helpers ───────────────────────────────────────────

    private String buildClickUrl(Payment payment) {
        return "https://my.click.uz/services/pay" +
               "?service_id=" + clickServiceId +
               "&merchant_id=" + clickMerchantId +
               "&amount=" + payment.getAmount() +
               "&transaction_param=" + payment.getId() +
               "&return_url=https://xorazmdelivery.uz/payment/success";
    }

    private String buildPaymeUrl(Payment payment) {
        // Payme URL base64 encoded params bilan
        String raw = "m=" + paymeMerchantId
                   + ";ac.order_id=" + payment.getOrder().getId()
                   + ";a=" + (payment.getAmount() * 100); // tiyin
        String encoded = java.util.Base64.getEncoder().encodeToString(raw.getBytes());
        return "https://checkout.paycom.uz/" + encoded;
    }

    private Map<String, Object> checkPerformTransaction(Map<String, Object> params) {
        return Map.of("result", Map.of("allow", true));
    }

    private Map<String, Object> createTransaction(Map<String, Object> params) {
        String id = (String) params.get("id");
        return Map.of("result", Map.of(
                "create_time", Instant.now().toEpochMilli(),
                "transaction", id,
                "state", 1
        ));
    }

    private Map<String, Object> performTransaction(Map<String, Object> params) {
        String id = (String) params.get("id");
        return Map.of("result", Map.of(
                "perform_time", Instant.now().toEpochMilli(),
                "transaction", id,
                "state", 2
        ));
    }

    private Map<String, Object> cancelTransaction(Map<String, Object> params) {
        String id = (String) params.get("id");
        return Map.of("result", Map.of(
                "cancel_time", Instant.now().toEpochMilli(),
                "transaction", id,
                "state", -1
        ));
    }
}
