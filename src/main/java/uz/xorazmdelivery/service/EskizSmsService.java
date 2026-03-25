package uz.xorazmdelivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Eskiz.uz SMS integratsiyasi.
 * Docs: https://documenter.getpostman.com/view/663428/2s93JtP3F6
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EskizSmsService {

    private final WebClient.Builder webClientBuilder;

    @Value("${eskiz.base-url}")
    private String baseUrl;

    @Value("${eskiz.email}")
    private String email;

    @Value("${eskiz.password}")
    private String password;

    @Value("${eskiz.sender}")
    private String sender;

    private volatile String cachedToken;

    /**
     * SMS yuborish asosiy metodi.
     * @param phone  +998XXXXXXXXX formatida
     * @param text   SMS matni
     */
    public void send(String phone, String text) {
        String normalizedPhone = phone.replace("+", "");
        getToken()
            .flatMap(token -> sendSms(token, normalizedPhone, text))
            .doOnError(e -> log.error("SMS yuborishda xato [{}]: {}", phone, e.getMessage()))
            .subscribe(
                r -> log.debug("SMS yuborildi: {}", phone),
                e -> log.error("SMS xatosi: {}", e.getMessage())
            );
    }

    private Mono<String> getToken() {
        if (cachedToken != null) return Mono.just(cachedToken);
        return webClientBuilder.build()
                .post()
                .uri(baseUrl + "/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", password))
                .retrieve()
                .bodyToMono(Map.class)
                .map(body -> {
                    var data = (Map<?, ?>) body.get("data");
                    String token = (String) data.get("token");
                    this.cachedToken = token;
                    return token;
                });
    }

    private Mono<Void> sendSms(String token, String phone, String text) {
        return webClientBuilder.build()
                .post()
                .uri(baseUrl + "/message/sms/send")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of(
                        "mobile_phone", phone,
                        "message", text,
                        "from", sender,
                        "callback_url", ""
                ))
                .retrieve()
                .bodyToMono(Void.class);
    }
}
