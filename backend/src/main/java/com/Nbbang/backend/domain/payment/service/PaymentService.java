package com.Nbbang.backend.domain.payment.service;

import com.Nbbang.backend.domain.auth.entity.UserAccount;
import com.Nbbang.backend.domain.auth.repository.UserAccountRepository;
import com.Nbbang.backend.domain.payment.dto.PaymentPrepareRequest;
import com.Nbbang.backend.domain.payment.dto.PaymentPrepareResponse;
import com.Nbbang.backend.domain.payment.dto.PaymentRequest;
import com.Nbbang.backend.domain.payment.dto.PaymentResponse;
import com.Nbbang.backend.domain.payment.entity.Payment;
import com.Nbbang.backend.domain.payment.repository.PaymentRepository;
import com.Nbbang.backend.domain.product.entity.Participation;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
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
    private final PlatformTransactionManager transactionManager; // [신규] 결제 취소/자동환불 saga용

    @Value("${toss.secret-key}")
    private String secretKey;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.tosspayments.com")
            .build();

    // 결제 준비: 결제창을 열기 전에 서버가 실제 상품 가격을 확인하고 PENDING 상태로 기록해둔다.
    // userId는 로그인 세션에서 검증된 값만 들어와야 함 (PaymentController에서 보장).
    // [수정] PRD-RQ-003: 정원 체크를 비관적 락으로 직렬화
    @Transactional
    public PaymentPrepareResponse prepare(PaymentPrepareRequest request, String userId) {
        Product product = productRepository.findByIdForUpdate(request.getProductId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

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

    // Toss 리다이렉트 콜백 처리: PENDING 조회 -> 금액 대조 -> 승인 확정(커밋) -> 참여 확정 시도.
    //
    // [수정] PRD-RQ-003: 예전엔 이 메서드 전체 + finalizeSuccessfulPayment가 하나의 @Transactional
    // 이었고, 같은 빈 안에서 this.finalizeSuccessfulPayment(...)를 호출했기 때문에(자기 자신 호출은
    // 프록시를 거치지 않아 @Transactional이 무시됨) 실제로는 트랜잭션이 하나였다. 그 결과 Toss 승인
    // (실제 돈 인출)까지 끝난 뒤 joinProduct가 정원 초과로 실패하면 전체 트랜잭션이 롤백되어
    // Payment.status=DONE 저장까지 함께 사라졌다 — 고객 돈은 빠져나갔는데 결제/환불 기록이 전혀
    // 남지 않는 상태가 될 수 있었음. TransactionTemplate으로 "DONE 커밋"과 "참여 확정 시도(+실패 시
    // 자동환불)"를 별도 트랜잭션으로 분리해 이 문제를 막는다.
    public PaymentResponse processSuccessCallback(String orderId, String paymentKey, Long amount) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        PaymentResponse cached = tx.execute(status -> {
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
            return null;
        });
        if (cached != null) {
            return cached;
        }

        PaymentRequest request = new PaymentRequest();
        request.setPaymentKey(paymentKey);
        request.setOrderId(orderId);
        request.setAmount(amount);
        PaymentResponse result = confirmPayment(request); // Toss 승인(실제 인출) — 트랜잭션 밖에서 호출

        Long paymentId = tx.execute(status -> {
            Payment payment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND));
            payment.setPaymentKey(result.getPaymentKey());
            payment.setStatus("DONE");
            payment.setApprovedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            return payment.getId();
        });

        // [신규] PRD-RQ-003: 결제는 승인됐지만 참여 확정이 실패하는 "고아 결제" 시나리오를
        // 자동 환불로 보상 처리한다 (예: 이 콜백이 처리되는 사이 다른 결제가 먼저 정원을 채운 경우).
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND));
        try {
            productService.joinProduct(payment.getProductId(), payment.getMember().getEmail());
        } catch (CustomException e) {
            log.warn("결제는 승인됐지만 참여 확정 실패, 자동 환불 처리: orderId={}, reason={}", orderId, e.getErrorCode());
            refundViaToss(payment.getId(), "정원 초과 자동 환불");
        }

        return result;
    }

    // [신규] PAY-RQ-001: Toss 공식 취소 API 호출 (confirmPayment와 동일한 인증/에러 처리 패턴)
    private PaymentResponse callTossCancel(String paymentKey, String reason) {
        String encodedKey = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        Map<String, Object> body = new HashMap<>();
        body.put("cancelReason", reason);

        try {
            return webClient.post()
                    .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                    .header("Authorization", "Basic " + encodedKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(PaymentResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Toss 결제 취소 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.PAYMENT_REFUND_FAILED);
        } catch (Exception e) {
            log.error("Toss 결제 취소 중 알 수 없는 오류", e);
            throw new CustomException(ErrorCode.PAYMENT_REFUND_FAILED);
        }
    }

    // [신규] PAY-RQ-001: 결제 취소 saga의 공통 부분 — CANCEL_REQUESTED 커밋 -> Toss 호출 ->
    // CANCELED/REFUND_FAILED 커밋. Participation/Product 인원 조정은 호출부 책임
    // (자동환불 시엔 Participation이 애초에 없고, 사용자 취소 시엔 성공 후에만 감소시켜야 하기 때문).
    // orderId/paymentKey 기준 재시도가 CANCEL_REQUESTED/REFUND_FAILED 상태에서 다시 들어와도
    // 동일하게 동작하므로 멱등하다.
    private void refundViaToss(Long paymentId, String reason) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        String paymentKey = tx.execute(status -> {
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND));
            payment.setStatus("CANCEL_REQUESTED");
            paymentRepository.save(payment);
            return payment.getPaymentKey();
        });

        try {
            callTossCancel(paymentKey, reason);
        } catch (CustomException e) {
            tx.executeWithoutResult(status -> {
                paymentRepository.findById(paymentId).ifPresent(payment -> {
                    payment.setStatus("REFUND_FAILED");
                    paymentRepository.save(payment);
                });
            });
            throw e;
        }

        tx.executeWithoutResult(status -> {
            paymentRepository.findById(paymentId).ifPresent(payment -> {
                payment.setStatus("CANCELED");
                payment.setCanceledAt(LocalDateTime.now());
                payment.setCancelReason(reason);
                paymentRepository.save(payment);
            });
        });
    }

    // [신규] PRD-RQ-001 + PAY-RQ-001: 구매자 본인의 참여 취소.
    // Participation을 소유자 기준으로 특정하고, 결제 완료 건이 있으면 Toss 환불이 성공한 뒤에만
    // Participation/currentCount를 확정적으로 정리한다 (환불 실패 시 인원/참여는 그대로 유지).
    public Product cancelParticipation(Long productId, String email, String reason) {
        Participation participation = participationRepository
                .findByProduct_ProductIdAndMember_Email(productId, email)
                .orElseThrow(() -> new CustomException(ErrorCode.PARTICIPATION_NOT_FOUND));

        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        // Phase 1: Product 잠금 + OPEN 상태 확인(=정산 시작 전) + 관련 결제 조회
        Payment payment = tx.execute(status -> {
            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
            if (!"OPEN".equals(product.getStatus())) {
                throw new CustomException(ErrorCode.PRODUCT_CANNOT_MODIFY_COMPLETED);
            }
            return paymentRepository
                    .findFirstByProductIdAndMember_EmailOrderByIdDesc(productId, email)
                    .orElse(null);
        });

        if (payment == null || "CANCELED".equals(payment.getStatus()) || "PENDING".equals(payment.getStatus())) {
            // 결제 기록이 없는 참여(레거시 무료 참여), 이미 취소 완료된 결제, 또는 아직 Toss 승인이
            // 확정되지 않은 PENDING 건(=아직 돈이 빠져나가지 않음, 환불 대상 아님) — 인원만 정리
            return tx.execute(status -> decrementAndRemoveParticipation(productId, participation.getId()));
        }

        if (!List.of("DONE", "CANCEL_REQUESTED", "REFUND_FAILED").contains(payment.getStatus())) {
            // 알 수 없는 상태값에 대한 방어적 처리
            throw new CustomException(ErrorCode.PARTICIPATION_NOT_FOUND);
        }

        // 환불 실패 시 refundViaToss가 PAYMENT_REFUND_FAILED를 던지며 여기서 중단되고,
        // Participation/currentCount는 그대로 유지된다 (PAY-RQ-001 인수 기준 2).
        refundViaToss(payment.getId(), reason);

        return tx.execute(status -> decrementAndRemoveParticipation(productId, participation.getId()));
    }

    private Product decrementAndRemoveParticipation(Long productId, Long participationId) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        product.decrementCurrentCount();
        participationRepository.deleteById(participationId);
        return product;
    }
}
