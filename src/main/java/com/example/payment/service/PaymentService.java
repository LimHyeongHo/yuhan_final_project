package com.example.payment.service;

import com.example.payment.dto.OrderInfo;
import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import com.example.payment.entity.Order;
import com.example.payment.entity.Participant;
import com.example.payment.entity.Payment;
import com.example.payment.repository.OrderRepository;
import com.example.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    @Value("${toss.secret-key}")
    private String secretKey;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.tosspayments.com")
            .build();

    @Transactional
    public PaymentResponse confirmPayment(PaymentRequest request) {
        String encodedKey = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        Map<String, Object> body = new HashMap<>();
        body.put("paymentKey", request.getPaymentKey());
        body.put("orderId", request.getOrderId());
        body.put("amount", request.getAmount());

        PaymentResponse response = webClient.post()
                .uri("/v1/payments/confirm")
                .header("Authorization", "Basic " + encodedKey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(PaymentResponse.class)
                .block();

        // DB에서 주문 조회 → 없으면 request 값 사용
        Order order = orderRepository.findById(request.getOrderId()).orElse(null);
        String orderName = (order != null) ? order.getOrderName()
                : (request.getOrderName() != null && !request.getOrderName().isBlank())
                        ? request.getOrderName() : "공동주문";
        int participants = (order != null) ? order.getActiveParticipants()
                : (request.getParticipants() > 0 ? request.getParticipants() : 1);
        long perPerson = response.getTotalAmount() / participants;

        // 결제 완료 시 DB 저장
        Payment payment = new Payment();
        payment.setPaymentKey(response.getPaymentKey());
        payment.setOrderId(response.getOrderId());
        payment.setOrderName(orderName);
        payment.setTotalAmount(response.getTotalAmount());
        payment.setPerPerson(perPerson);
        payment.setStatus(response.getStatus());
        payment.setMethod(response.getMethod());
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        return response;
    }

    public Map<String, Object> calculateAmount(Long totalAmount, int participants) {
        long perPerson = totalAmount / participants;
        long remainder = totalAmount % participants;

        Map<String, Object> result = new HashMap<>();
        result.put("totalAmount", totalAmount);
        result.put("participants", participants);
        result.put("perPerson", perPerson);
        result.put("remainder", remainder);
        return result;
    }

    @Transactional
    public void saveOrder(String orderId, String orderName, Long totalAmount, int participants, LocalDateTime deadline) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setOrderName(orderName);
        order.setTotalAmount(totalAmount);
        order.setTotalParticipants(participants);
        order.setActiveParticipants(participants);
        order.setDeadline(deadline);

        for (int i = 1; i < participants; i++) {
            Participant p = new Participant();
            p.setParticipantId(i);
            p.setName("참여자 " + i);
            p.setCancelled(false);
            p.setUnpaid(false);
            p.setOrder(order);
            order.getParticipants().add(p);
        }
        orderRepository.save(order);
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void checkDeadline() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> expiredOrders = orderRepository.findByDeadlineBefore(now);

        for (Order order : expiredOrders) {
            for (Participant p : order.getParticipants()) {
                if (!p.isCancelled() && !p.isUnpaid()) {
                    p.setUnpaid(true);
                    p.setCancelled(true);
                    order.setActiveParticipants(order.getActiveParticipants() - 1);
                    System.out.println("미입금 자동 제외: " + p.getName() + " / 주문: " + order.getOrderId());
                }
            }
        }
        orderRepository.saveAll(expiredOrders);
    }

    public OrderInfo getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .map(this::toOrderInfo)
                .orElse(null);
    }

    private OrderInfo toOrderInfo(Order order) {
        OrderInfo info = new OrderInfo();
        info.setOrderId(order.getOrderId());
        info.setTotalAmount(order.getTotalAmount());
        info.setTotalParticipants(order.getTotalParticipants());
        info.setActiveParticipants(order.getActiveParticipants());
        info.setDeadline(order.getDeadline());

        List<OrderInfo.ParticipantInfo> participants = order.getParticipants().stream()
                .map(p -> {
                    OrderInfo.ParticipantInfo pi = new OrderInfo.ParticipantInfo();
                    pi.setId(p.getParticipantId());
                    pi.setName(p.getName());
                    pi.setCancelled(p.isCancelled());
                    pi.setUnpaid(p.isUnpaid());
                    return pi;
                })
                .collect(Collectors.toList());
        info.setParticipants(participants);
        return info;
    }
}
