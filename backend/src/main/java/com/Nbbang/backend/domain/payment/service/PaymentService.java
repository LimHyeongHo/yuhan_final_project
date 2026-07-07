package com.Nbbang.backend.domain.payment.service;

import com.Nbbang.backend.domain.payment.dto.PaymentRequest;
import com.Nbbang.backend.domain.payment.dto.PaymentResponse;
// import com.Nbbang.backend.global.exception.CustomException;
// import com.Nbbang.backend.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {

    @Value("${toss.secret-key}")
    private String secretKey;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.tosspayments.com")
            .build();

    public PaymentResponse confirmPayment(PaymentRequest request) {
        // TODO: global/exception 병합 후 CustomException으로 교체
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new RuntimeException("결제 금액이 올바르지 않습니다");
        }

        String encodedKey = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        Map<String, Object> body = new HashMap<>();
        body.put("paymentKey", request.getPaymentKey());
        body.put("orderId", request.getOrderId());
        body.put("amount", request.getAmount());

        try {
            return webClient.post()
                    .uri("/v1/payments/confirm")
                    .header("Authorization", "Basic " + encodedKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(PaymentResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new RuntimeException("결제 서버와 통신에 실패했습니다. 잠시 후 다시 시도해주세요");
        } catch (Exception e) {
            throw new RuntimeException("결제 승인에 실패했습니다. 잠시 후 다시 시도해주세요");
        }
    }
}
