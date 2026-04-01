package com.example.payment.service;


import com.example.payment.dto.OrderInfo;
import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PaymentService {

    @Value("${toss.secret-key}")
    private String secretKey;


    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.tosspayments.com")
            .build();


    public PaymentResponse confirmPayment(PaymentRequest request){

        String encodedKey = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        Map<String, Object> body  = new HashMap<>();
        body.put("paymentKey",request.getPaymentKey());
        body.put("orderId",request.getOrderId());
        body.put("amount",request.getAmount());



        return webClient.post()
                .uri("/v1/payments/confirm")
                .header("Authorization", "Basic " + encodedKey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(PaymentResponse.class)
                .block();
    }

    public Map<String, Object> calculateAmount(Long totalAmount, int participants){
        long perPerson = totalAmount / participants;
        long remainder = totalAmount % participants;

        Map<String, Object> result = new HashMap<>();
        result.put("totalAmount", totalAmount);
        result.put("participants", participants);
        result.put("perPerson", perPerson);
        result.put("remainder", remainder);
        return result;
    }


    // 주문 정보 메모리 저장
    private final Map<String, OrderInfo> orderStorage = new ConcurrentHashMap<>();
    private final Set<String> processedOrders = new HashSet<>();

    // 주문 저장
    public void saveOrder(String orderId, Long totalAmount, int participants, LocalDateTime deadline) {
        OrderInfo order = new OrderInfo();
        order.setOrderId(orderId);
        order.setTotalAmount(totalAmount);
        order.setTotalParticipants(participants);
        order.setActiveParticipants(participants);
        order.setDeadline(deadline);

        // 참여자 목록 생성
        for (int i = 1; i < participants; i++) {
            OrderInfo.ParticipantInfo p = new OrderInfo.ParticipantInfo();
            p.setId(i);
            p.setName("참여자 " + i);
            p.setCancelled(false);
            p.setUnpaid(false);
            order.getParticipants().add(p);
        }
        orderStorage.put(orderId, order);
    }

    // 마감 시간 지난 미입금자 자동 제외
    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    public void checkDeadline() {
        LocalDateTime now = LocalDateTime.now();

        orderStorage.forEach((orderId, order) -> {
            if (order.getDeadline() != null && now.isAfter(order.getDeadline())) {
                order.getParticipants().forEach(p -> {
                    if (!p.isCancelled() && !p.isUnpaid()) {
                        p.setUnpaid(true); // 미입금 처리
                        p.setCancelled(true); // 자동 제외
                        order.setActiveParticipants(order.getActiveParticipants() - 1);
                        System.out.println("미입금 자동 제외: " + p.getName() + " / 주문: " + orderId);
                    }
                });
            }
        });
    }

    // 주문 조회
    public OrderInfo getOrder(String orderId) {
        return orderStorage.get(orderId);
    }
}
