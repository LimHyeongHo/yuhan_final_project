package com.example.payment.controller;
import com.example.payment.dto.OrderInfo;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import com.example.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;  // ✅ 올바른 것import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${app.base-url}")  // 이거 추가
    private String baseUrl;

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestBody PaymentRequest request){

        try{
            PaymentResponse response = paymentService.confirmPayment(request);
            return ResponseEntity.ok(response);
        }catch(Exception e){
            return ResponseEntity.badRequest().body("결제 실패: "+e.getMessage());
        }
    }

    @GetMapping("/calculate")
    public ResponseEntity<?> calculateAmount(
            @RequestParam Long totalAmount,
            @RequestParam int participants){

        try{
            Map<String, Object> result = paymentService.calculateAmount(totalAmount, participants);
            return ResponseEntity.ok(result);
        }catch(Exception e){
            return ResponseEntity.badRequest().body("계산 실패: " + e.getMessage());
        }
    }

    @GetMapping("/success")
    public void paymentSuccess(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam Long amount,
            @RequestParam(required = false, defaultValue = "공동주문") String orderName,
            @RequestParam(required = false, defaultValue = "4") int participants,
            HttpServletResponse response) throws IOException {

        try {
            // 결제 승인 요청
            PaymentRequest request = new PaymentRequest();
            request.setPaymentKey(paymentKey);
            request.setOrderId(orderId);
            request.setAmount(amount);
            request.setOrderName(orderName);
            request.setParticipants(participants);
            paymentService.confirmPayment(request);

            // 성공 페이지로 이동
            response.sendRedirect(baseUrl + "/success.html?amount=" + amount
                    + "&orderName=" + java.net.URLEncoder.encode(orderName, StandardCharsets.UTF_8)
                    + "&participants=" + participants);

        } catch (Exception e) {
            response.sendRedirect("/fail.html?message=" + e.getMessage());
        }
    }

    @GetMapping("/fail")
    public ResponseEntity<?> paymentFail(
            @RequestParam String message,
            @RequestParam String code){
        return ResponseEntity.badRequest()
                .body("결제 실패 - 사유: "+message+" 코드: "+code);
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancelParticipant(
            @RequestParam Long totalAmount,
            @RequestParam int currentParticipants) {
        try {
            if (currentParticipants <= 1) {
                return ResponseEntity.badRequest().body("참여자가 1명 이하면 취소 불가합니다.");
            }
            Map<String, Object> result =
                    paymentService.calculateAmount(totalAmount, currentParticipants - 1);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("취소 실패: " + e.getMessage());
        }
    }

    // 주문 저장 API
    @PostMapping("/save")
    public ResponseEntity<?> saveOrder(
            @RequestParam String orderId,
            @RequestParam String orderName,
            @RequestParam Long totalAmount,
            @RequestParam int participants,
            @RequestParam int deadlineMinutes) {
        try {
            LocalDateTime deadline = LocalDateTime.now().plusMinutes(deadlineMinutes);
            paymentService.saveOrder(orderId, orderName, totalAmount, participants, deadline);
            return ResponseEntity.ok("주문 저장 완료. 마감: " + deadline);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("저장 실패: " + e.getMessage());
        }
    }

    // 주문 상태 조회 API
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable String orderId) {
        try {
            OrderInfo order = paymentService.getOrder(orderId);
            if (order == null) return ResponseEntity.badRequest().body("주문 없음");
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("조회 실패: " + e.getMessage());
        }
    }
}
