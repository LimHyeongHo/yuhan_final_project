package com.Nbbang.backend.domain.payment.controller;

import com.Nbbang.backend.domain.payment.dto.PaymentPrepareRequest;
import com.Nbbang.backend.domain.payment.dto.PaymentPrepareResponse;
import com.Nbbang.backend.domain.payment.dto.PaymentResponse;
import com.Nbbang.backend.domain.payment.service.PaymentService;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    // 결제 준비: Toss 결제창을 열기 전에 서버가 가격을 검증하고 PENDING 기록을 남긴다.
    // 로그인 세션이 있어야만 진행 가능 (참여자 계정을 클라이언트 입력이 아닌 세션 기준으로 기록하기 위함).
    @PostMapping("/prepare")
    public ResponseEntity<PaymentPrepareResponse> preparePayment(@RequestBody PaymentPrepareRequest request,
                                                                    HttpServletRequest httpRequest) {
        String userId = requireUserId(httpRequest);
        PaymentPrepareResponse response = paymentService.prepare(request, userId);
        return ResponseEntity.ok(response);
    }

    private String requireUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        return (String) session.getAttribute("userId");
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
            PaymentResponse result = paymentService.processSuccessCallback(orderId, paymentKey, amount);

            response.sendRedirect(frontendUrl + "/payment/success"
                    + "?amount=" + amount
                    + "&orderName=" + URLEncoder.encode(orderName, StandardCharsets.UTF_8)
                    + "&orderId=" + orderId
                    + "&method=" + URLEncoder.encode(
                            result.getMethod() != null ? result.getMethod() : "", StandardCharsets.UTF_8));
        } catch (CustomException e) {
            ErrorCode errorCode = paymentRedirectError(e.getErrorCode());
            log.warn("결제 성공 콜백 처리 실패: orderId={}, code={}", orderId, errorCode.name());
            response.sendRedirect(frontendUrl + "/payment/fail?code=" + errorCode.name());
        } catch (Exception e) {
            log.error("결제 성공 콜백 처리 중 예상치 못한 오류: orderId={}, type={}",
                    orderId, e.getClass().getSimpleName());
            response.sendRedirect(frontendUrl + "/payment/fail?code="
                    + ErrorCode.PAYMENT_CONFIRM_FAILED.name());
        }
    }

    // 토스페이먼츠 → 백엔드 콜백 (결제 실패)
    @GetMapping("/fail")
    public void paymentFail(
            @RequestParam(required = false, defaultValue = "") String code,
            HttpServletResponse response) throws IOException {

        ErrorCode errorCode = "PAY_PROCESS_CANCELED".equals(code)
                ? ErrorCode.PAYMENT_CANCELLED
                : ErrorCode.PAYMENT_CONFIRM_FAILED;
        response.sendRedirect(frontendUrl + "/payment/fail?code=" + errorCode.name());
    }

    private ErrorCode paymentRedirectError(ErrorCode errorCode) {
        return switch (errorCode) {
            case PAYMENT_INVALID_AMOUNT,
                 PAYMENT_ORDER_NOT_FOUND,
                 PAYMENT_AMOUNT_MISMATCH,
                 PAYMENT_CANCELLED,
                 PAYMENT_CONFIRM_FAILED,
                 PAYMENT_REFUND_FAILED,
                 PAYMENT_JOIN_FAILED,
                 PAYMENT_CANCEL_IN_PROGRESS,
                 PAYMENT_LOOKUP_FAILED -> errorCode;
            default -> ErrorCode.PAYMENT_CONFIRM_FAILED;
        };
    }
}
