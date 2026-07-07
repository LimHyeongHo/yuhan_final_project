package com.Nbbang.backend.domain.payment.controller;

import com.Nbbang.backend.domain.payment.dto.PaymentRequest;
import com.Nbbang.backend.domain.payment.dto.PaymentResponse;
import com.Nbbang.backend.domain.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    // 토스페이먼츠 결제 승인
    @PostMapping("/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(@RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.confirmPayment(request);
        return ResponseEntity.ok(response);
    }

    // 토스페이먼츠 → 백엔드 콜백 (결제 성공)
    @GetMapping("/success")
    public void paymentSuccess(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam Long amount,
            @RequestParam(required = false, defaultValue = "") String orderName,
            HttpServletResponse response) throws IOException {

        try {
            PaymentRequest request = new PaymentRequest();
            request.setPaymentKey(paymentKey);
            request.setOrderId(orderId);
            request.setAmount(amount);
            PaymentResponse result = paymentService.confirmPayment(request);

            response.sendRedirect(frontendUrl + "/payment/success"
                    + "?amount=" + amount
                    + "&orderName=" + URLEncoder.encode(orderName, StandardCharsets.UTF_8)
                    + "&orderId=" + orderId
                    + "&method=" + URLEncoder.encode(
                            result.getMethod() != null ? result.getMethod() : "", StandardCharsets.UTF_8));
        } catch (Exception e) {
            response.sendRedirect(frontendUrl + "/payment/fail?message="
                    + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }

    // 토스페이먼츠 → 백엔드 콜백 (결제 실패)
    @GetMapping("/fail")
    public void paymentFail(
            @RequestParam(required = false, defaultValue = "결제가 취소되었습니다.") String message,
            @RequestParam(required = false, defaultValue = "") String code,
            HttpServletResponse response) throws IOException {

        response.sendRedirect(frontendUrl + "/payment/fail"
                + "?message=" + URLEncoder.encode(message, StandardCharsets.UTF_8)
                + "&code=" + code);
    }
}
