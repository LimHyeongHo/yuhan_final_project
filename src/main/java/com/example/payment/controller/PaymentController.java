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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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


    // 인증서 정보 조회 API
    @GetMapping("/cert-info")
    public ResponseEntity<?> getCertInfo() {
        try {
            Map<String, Object> certInfo = paymentService.getRealCertInfo();
            return ResponseEntity.ok(certInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("조회 실패: " + e.getMessage());
        }
    }

    @GetMapping("/cert-verify")
    public ResponseEntity<?> verifyCert() {
        try {
            List<Map<String, Object>> steps = new ArrayList<>();

            Map<String, Object> step1 = new HashMap<>();
            step1.put("step", "1");
            step1.put("title", "브라우저가 서버 접속");
            step1.put("description", "클라이언트가 https://localhost:8443 접속 요청");
            step1.put("status", "success");
            steps.add(step1);

            Map<String, Object> step2 = new HashMap<>();
            step2.put("step", "2");
            step2.put("title", "서버가 인증서 제출");
            step2.put("description", "서버가 Yuhan Root CA가 발급한 인증서 전달");
            step2.put("status", "success");
            steps.add(step2);

            Map<String, Object> step3 = new HashMap<>();
            step3.put("step", "3");
            step3.put("title", "CA 서명 검증");
            step3.put("description", "Yuhan Root CA의 공개키로 서버 인증서 서명 검증");
            step3.put("status", "success");
            steps.add(step3);

            Map<String, Object> step4 = new HashMap<>();
            step4.put("step", "4");
            step4.put("title", "유효기간 확인");
            step4.put("description", "인증서 유효기간 2026-04-03 ~ 2027-04-03 확인");
            step4.put("status", "success");
            steps.add(step4);

            Map<String, Object> step5 = new HashMap<>();
            step5.put("step", "5");
            step5.put("title", "암호화 통신 시작");
            step5.put("description", "검증 완료 후 HTTPS 암호화 통신 시작");
            step5.put("status", "success");
            steps.add(step5);

            return ResponseEntity.ok(steps);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("검증 실패: " + e.getMessage());
        }
    }

    @GetMapping("/cert-expired")
    public ResponseEntity<?> certExpired() {
        try {
            Map<String, Object> certInfo = paymentService.getExpiredCertInfo();
            return ResponseEntity.ok(certInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("조회 실패: " + e.getMessage());
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
            @RequestParam Long totalAmount,
            @RequestParam int participants,
            @RequestParam int deadlineMinutes) {
        try {
            LocalDateTime deadline = LocalDateTime.now().plusMinutes(deadlineMinutes);
            paymentService.saveOrder(orderId, totalAmount, participants, deadline);
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
