package com.Nbbang.backend.domain.payment.service;

import com.Nbbang.backend.domain.auth.entity.UserAccount;
import com.Nbbang.backend.domain.auth.repository.UserAccountRepository;
import com.Nbbang.backend.domain.payment.dto.PaymentPrepareRequest;
import com.Nbbang.backend.domain.payment.dto.PaymentPrepareResponse;
import com.Nbbang.backend.domain.payment.dto.PaymentRequest;
import com.Nbbang.backend.domain.payment.dto.PaymentResponse;
import com.Nbbang.backend.domain.payment.entity.Payment;
import com.Nbbang.backend.domain.payment.repository.PaymentRepository;
import com.Nbbang.backend.domain.product.entity.Product;
import com.Nbbang.backend.domain.product.repository.ParticipationRepository;
import com.Nbbang.backend.domain.product.repository.ProductRepository;
import com.Nbbang.backend.domain.product.service.ProductService;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final ProductService productService;
    private final UserAccountRepository userAccountRepository;
    private final ParticipationRepository participationRepository;

    @Value("${toss.secret-key}")
    private String secretKey;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.tosspayments.com")
            .build();

    // 결제 준비: 결제창을 열기 전에 서버가 실제 상품 가격을 확인하고 PENDING 상태로 기록해둔다.
    // userId는 로그인 세션에서 검증된 값만 들어와야 함 (PaymentController에서 보장).
    public PaymentPrepareResponse prepare(PaymentPrepareRequest request, String userId) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // [MEM-RQ-002] 판매자가 탈퇴한 상품은 결제 준비 단계에서부터 차단 (참여 시점만 막으면 이미 결제된 뒤라 늦음)
        if ("SELLER_WITHDRAWN".equals(product.getStatus())) {
            throw new CustomException(ErrorCode.PRODUCT_SELLER_WITHDRAWN);
        }

        if (product.getCurrentCount() != null && product.getCurrentCount() >= product.getTargetCount()) {
            throw new CustomException(ErrorCode.PURCHASE_FULL);
        }

        if (product.getDeadline() != null && product.getDeadline().isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.PURCHASE_DEADLINE_PASSED);
        }

        // 결제 전에 미리 막아야 함 - joinProduct에서만 체크하면 이미 결제(Toss 승인)된 뒤에 거절하게 됨
        if (participationRepository.existsByProduct_ProductIdAndMember_Email(product.getProductId(), userId)) {
            throw new CustomException(ErrorCode.PURCHASE_ALREADY_JOINED);
        }

        UserAccount member = userAccountRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_UNAUTHORIZED));

        Long amount = product.getPrice().longValue();
        String orderId = "order_" + UUID.randomUUID().toString().replace("-", "");

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setProductId(product.getProductId());
        payment.setMember(member);
        payment.setBuyerName(member.getNickname());
        payment.setAmount(amount);
        payment.setStatus("PENDING");
        paymentRepository.save(payment);

        return new PaymentPrepareResponse(orderId, amount);
    }

    public PaymentResponse confirmPayment(PaymentRequest request) {
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new CustomException(ErrorCode.PAYMENT_INVALID_AMOUNT);
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
            log.error("Toss 결제 승인 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        } catch (Exception e) {
            log.error("Toss 결제 승인 중 알 수 없는 오류", e);
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }
    }

    // Toss 리다이렉트 콜백 처리: PENDING 조회 -> 금액 대조 -> 승인 확정 -> 참여 확정까지 한 번에 묶는다.
    @Transactional
    public PaymentResponse processSuccessCallback(String orderId, String paymentKey, Long amount) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND));

        if ("DONE".equals(payment.getStatus())) {
            // 이미 처리된 콜백(중복 리다이렉트 등) - 재처리하지 않고 그대로 성공 응답
            PaymentResponse response = new PaymentResponse();
            response.setPaymentKey(payment.getPaymentKey());
            response.setOrderId(payment.getOrderId());
            response.setTotalAmount(payment.getAmount());
            response.setStatus("DONE");
            return response;
        }

        if (!payment.getAmount().equals(amount)) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        PaymentRequest request = new PaymentRequest();
        request.setPaymentKey(paymentKey);
        request.setOrderId(orderId);
        request.setAmount(amount);
        PaymentResponse result = confirmPayment(request);

        finalizeSuccessfulPayment(payment, result);

        return result;
    }

    // 결제 확정을 참여 확정으로 연결: Payment를 DONE으로 갱신하고 참여 인원/기록을 반영한다.
    @Transactional
    public void finalizeSuccessfulPayment(Payment payment, PaymentResponse tossResponse) {
        payment.setPaymentKey(tossResponse.getPaymentKey());
        payment.setStatus("DONE");
        payment.setApprovedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        productService.joinProduct(payment.getProductId(), payment.getMember().getEmail());
    }
}
