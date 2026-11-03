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
import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
    private final PlatformTransactionManager transactionManager;

    @Value("${toss.secret-key}")
    private String secretKey;

    // Toss API 클라이언트 (연결 3초, 응답 10초 타임아웃)
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.tosspayments.com")
            .clientConnector(new ReactorClientHttpConnector(
                    HttpClient.create()
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                            .responseTimeout(Duration.ofSeconds(10))))
            .build();

    // PaymentController.preparePayment() 호출 → 상품 락 걸고 정원/마감/중복 참여 확인 후 PENDING 결제 생성
    @Transactional
    public PaymentPrepareResponse prepare(PaymentPrepareRequest request, String userId) {
        Product product = productRepository.findByIdForUpdate(request.getProductId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        if ("SELLER_WITHDRAWN".equals(product.getStatus())) {
            throw new CustomException(ErrorCode.PRODUCT_SELLER_WITHDRAWN);
        }

        if (product.getCurrentCount() != null && product.getCurrentCount() >= product.getTargetCount()) {
            throw new CustomException(ErrorCode.PURCHASE_FULL);
        }

        if (product.getDeadline() != null && product.getDeadline().isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.PURCHASE_DEADLINE_PASSED);
        }

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

    // Toss 결제 승인 API 호출 (실제 인출)
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

    // orderId로 Toss 결제 상태 조회. 주문이 없으면 null
    private PaymentResponse lookupTossPayment(String orderId) {
        String encodedKey = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        try {
            return webClient.get()
                    .uri("/v1/payments/orders/{orderId}", orderId)
                    .header("Authorization", "Basic " + encodedKey)
                    .retrieve()
                    .bodyToMono(PaymentResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return null;
            }
            log.error("Toss 결제조회 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.PAYMENT_LOOKUP_FAILED);
        } catch (Exception e) {
            log.error("Toss 결제조회 중 알 수 없는 오류", e);
            throw new CustomException(ErrorCode.PAYMENT_LOOKUP_FAILED);
        }
    }

    // 승인 실패 시 Toss 조회로 이미 승인됐는지 확인 (응답 유실 대비)
    private PaymentResponse confirmOrReconcile(PaymentRequest request, Long expectedAmount) {
        try {
            return confirmPayment(request);
        } catch (CustomException e) {
            PaymentResponse looked = lookupTossPayment(request.getOrderId());
            if (looked != null && "DONE".equals(looked.getStatus())
                    && expectedAmount.equals(looked.getTotalAmount())) {
                log.warn("[PAYMENT-AUDIT] confirm 실패했으나 Toss 조회상 이미 승인됨 - 조회 결과로 진행: orderId={}",
                        request.getOrderId());
                return looked;
            }
            throw e;
        }
    }

    // PaymentController.paymentSuccess() 호출 → PENDING → Toss 승인(APPROVED) → ProductService.joinProduct() → DONE
    // 참여 확정 실패 시 자동 환불. 재호출되면 현재 상태부터 이어서 처리
    public PaymentResponse processSuccessCallback(String orderId, String paymentKey, Long amount) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        String phase = tx.execute(status -> {
            Payment payment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND));
            switch (payment.getStatus()) {
                case "DONE":
                    return "DONE";
                case "CANCELED":
                    throw new CustomException(ErrorCode.PAYMENT_JOIN_FAILED);
                case "CANCEL_REQUESTED":
                case "REFUND_FAILED":
                    return "RESUME_REFUND";
                case "APPROVED":
                    return "FINALIZE";
                case "PENDING":
                    if (!payment.getAmount().equals(amount)) {
                        throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
                    }
                    return "CONFIRM";
                default:
                    throw new CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND);
            }
        });

        if ("DONE".equals(phase)) {
            return doneResponse(orderId);
        }

        if ("RESUME_REFUND".equals(phase)) {
            Long paymentId = tx.execute(status -> paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND)).getId());
            log.warn("이전 콜백에서 시작된 자동 환불을 재개: orderId={}", orderId);
            refundViaToss(paymentId, "참여 확정 실패 자동 환불(재개)");
            throw new CustomException(ErrorCode.PAYMENT_JOIN_FAILED);
        }

        String method = null;
        if ("CONFIRM".equals(phase)) {
            PaymentRequest request = new PaymentRequest();
            request.setPaymentKey(paymentKey);
            request.setOrderId(orderId);
            request.setAmount(amount);
            PaymentResponse confirmed = confirmOrReconcile(request, amount);
            method = confirmed.getMethod();

            tx.executeWithoutResult(status -> {
                Payment payment = paymentRepository.findByOrderId(orderId)
                        .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND));
                payment.setPaymentKey(confirmed.getPaymentKey());
                payment.setApprovedAt(LocalDateTime.now());
                payment.setStatus("APPROVED");
                paymentRepository.save(payment);
            });
            log.info("[PAYMENT-AUDIT] Toss 승인 완료(APPROVED): orderId={}, amount={}", orderId, amount);
        }

        long[] ref = tx.execute(status -> {
            Payment payment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND));
            return new long[]{payment.getId(), payment.getProductId()};
        });
        Long paymentId = ref[0];
        Long productId = ref[1];
        String buyerEmail = tx.execute(status -> paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND))
                .getMember().getEmail());

        boolean alreadyJoined = isJoinedByThisPayment(productId, buyerEmail, paymentId);

        if (!alreadyJoined) {
            try {
                productService.joinProduct(productId, buyerEmail);
            } catch (CustomException e) {
                // 동시 콜백이 먼저 참여를 확정한 경우는 환불하지 않음
                if (e.getErrorCode() == ErrorCode.PURCHASE_ALREADY_JOINED
                        && isJoinedByThisPayment(productId, buyerEmail, paymentId)) {
                    log.warn("[PAYMENT-AUDIT] 동시 콜백이 먼저 참여 확정 - 환불 없이 진행: orderId={}", orderId);
                } else {
                    log.warn("결제는 승인됐지만 참여 확정 실패, 자동 환불: orderId={}, reason={}", orderId, e.getErrorCode());
                    refundViaToss(paymentId, "참여 확정 실패 자동 환불");
                    throw new CustomException(ErrorCode.PAYMENT_JOIN_FAILED);
                }
            }
        }

        tx.executeWithoutResult(status -> paymentRepository.findById(paymentId).ifPresent(payment -> {
            if (!"DONE".equals(payment.getStatus())) {
                payment.setStatus("DONE");
                paymentRepository.save(payment);
                log.info("[PAYMENT-AUDIT] 결제/참여 확정(DONE): orderId={}, productId={}, buyer={}",
                        orderId, productId, buyerEmail);
            }
        }));

        return doneResponse(orderId, method);
    }

    // 참여가 이 결제 몫인지 확인 (같은 구매자의 다른 DONE 결제가 있으면 이중 결제)
    private boolean isJoinedByThisPayment(Long productId, String buyerEmail, Long paymentId) {
        return participationRepository.existsByProduct_ProductIdAndMember_Email(productId, buyerEmail)
                && !paymentRepository.existsByProductIdAndMember_EmailAndStatusInAndIdNot(
                        productId, buyerEmail, List.of("DONE"), paymentId);
    }

    private PaymentResponse doneResponse(String orderId) {
        return doneResponse(orderId, null);
    }

    private PaymentResponse doneResponse(String orderId, String method) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Payment payment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND));
            PaymentResponse response = new PaymentResponse();
            response.setPaymentKey(payment.getPaymentKey());
            response.setOrderId(payment.getOrderId());
            response.setTotalAmount(payment.getAmount());
            response.setStatus("DONE");
            response.setMethod(method);
            return response;
        });
    }

    // Toss 결제 취소 API 호출
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

    // 환불 공통 처리: CANCEL_REQUESTED → callTossCancel() → CANCELED / 실패 시 REFUND_FAILED
    // 참여·인원 정리는 호출한 쪽에서 처리
    private void refundViaToss(Long paymentId, String reason) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        String[] keyAndOrder = tx.execute(status -> {
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND));
            payment.setStatus("CANCEL_REQUESTED");
            payment.setCancelRequestedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            return new String[]{payment.getPaymentKey(), payment.getOrderId()};
        });
        String paymentKey = keyAndOrder[0];

        // Toss에 이미 취소된 건은 상태만 맞춤
        PaymentResponse looked = lookupTossPayment(keyAndOrder[1]);
        if (looked != null && ("CANCELED".equals(looked.getStatus()) || "PARTIAL_CANCELED".equals(looked.getStatus()))) {
            log.warn("[PAYMENT-AUDIT] Toss 조회상 이미 취소됨 - 로컬 상태만 CANCELED로 동기화: paymentId={}", paymentId);
            markCanceled(tx, paymentId, reason);
            return;
        }

        try {
            callTossCancel(paymentKey, reason);
        } catch (CustomException e) {
            tx.executeWithoutResult(status -> {
                paymentRepository.findById(paymentId).ifPresent(payment -> {
                    payment.setStatus("REFUND_FAILED");
                    paymentRepository.save(payment);
                });
            });
            log.error("[PAYMENT-AUDIT] 환불 실패(REFUND_FAILED): paymentId={}, reason={}", paymentId, reason);
            throw e;
        }

        markCanceled(tx, paymentId, reason);
        log.info("[PAYMENT-AUDIT] 환불 완료(CANCELED): paymentId={}, reason={}", paymentId, reason);
    }

    private void markCanceled(TransactionTemplate tx, Long paymentId, String reason) {
        tx.executeWithoutResult(status -> paymentRepository.findById(paymentId).ifPresent(payment -> {
            payment.setStatus("CANCELED");
            payment.setCanceledAt(LocalDateTime.now());
            payment.setCancelReason(reason);
            paymentRepository.save(payment);
        }));
    }

    private static final List<String> ORPHAN_PRODUCT_STATUSES = List.of("CLOSED_FAIL", "SELLER_WITHDRAWN");

    // PaymentRefundScheduler 호출 → 상품 삭제/모집 실패/판매자 탈퇴 공구의 미환불 결제 조회
    public List<Long> findOrphanedPaymentIds() {
        return paymentRepository.findOrphanedPayments(
                        List.of("DONE", "APPROVED", "CANCEL_REQUESTED"), ORPHAN_PRODUCT_STATUSES)
                .stream()
                .filter(this::isOrphanRefundTarget)
                .map(Payment::getId)
                .toList();
    }

    // APPROVED, CANCEL_REQUESTED는 10분 지난 건만 대상 (REFUND_FAILED는 자동 재시도 안 함)
    private boolean isOrphanRefundTarget(Payment payment) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
        return switch (payment.getStatus()) {
            case "DONE" -> true;
            case "APPROVED" -> payment.getApprovedAt() != null && payment.getApprovedAt().isBefore(cutoff);
            case "CANCEL_REQUESTED" -> payment.getCancelRequestedAt() == null
                    || payment.getCancelRequestedAt().isBefore(cutoff);
            default -> false;
        };
    }

    // PaymentRefundScheduler 호출 → 건별 자동 환불
    public void refundOrphanedPayment(Long paymentId) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        String reason = tx.execute(status -> {
            Payment payment = paymentRepository.findById(paymentId).orElse(null);
            if (payment == null || !isOrphanRefundTarget(payment)) {
                return null;
            }
            String productStatus = productRepository.findById(payment.getProductId())
                    .map(Product::getStatus)
                    .orElse(null);
            if (productStatus == null) {
                return "상품 삭제로 인한 자동 환불";
            }
            if ("CLOSED_FAIL".equals(productStatus)) {
                return "공동구매 모집 실패 자동 환불";
            }
            if ("SELLER_WITHDRAWN".equals(productStatus)) {
                return "판매자 탈퇴로 인한 자동 환불";
            }
            return null;
        });
        if (reason == null) {
            return;
        }

        log.info("[PAYMENT-AUDIT] 종료된 공구 자동 환불 시작: paymentId={}, reason={}", paymentId, reason);
        try {
            refundViaToss(paymentId, reason);
        } catch (CustomException e) {
            tx.executeWithoutResult(status -> paymentRepository.findById(paymentId).ifPresent(payment -> {
                if ("CANCEL_REQUESTED".equals(payment.getStatus())) {
                    payment.setStatus("REFUND_FAILED");
                    paymentRepository.save(payment);
                }
            }));
            log.error("[PAYMENT-AUDIT] 종료된 공구 자동 환불 실패(REFUND_FAILED): paymentId={}, code={}",
                    paymentId, e.getErrorCode());
        }
    }

    // ProductController.cancelJoinProduct() 호출 → 환불 대상 확정 → refundViaToss() → 참여/인원 정리
    // 환불 실패 시 참여와 인원은 그대로 유지
    public Product cancelParticipation(Long productId, String email, String reason) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        // 1. 상품 락 안에서 CANCEL_REQUESTED 선점 (중복 취소 방지). 환불 불필요하면 바로 정리 후 null
        Long refundPaymentId = tx.execute(status -> {
            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
            if (!"OPEN".equals(product.getStatus())) {
                throw new CustomException(ErrorCode.PRODUCT_CANNOT_MODIFY_COMPLETED);
            }

            Participation participation = participationRepository
                    .findByProduct_ProductIdAndMember_Email(productId, email)
                    .orElseThrow(() -> new CustomException(ErrorCode.PARTICIPATION_NOT_FOUND));

            // 인출된 결제 우선
            Payment payment = paymentRepository
                    .findFirstByProductIdAndMember_EmailAndStatusInOrderByIdDesc(productId, email,
                            List.of("APPROVED", "DONE", "REFUND_FAILED", "CANCEL_REQUESTED"))
                    .or(() -> paymentRepository.findFirstByProductIdAndMember_EmailOrderByIdDesc(productId, email))
                    .orElse(null);
            String st = payment == null ? null : payment.getStatus();

            if (payment == null || "CANCELED".equals(st) || "PENDING".equals(st)) {
                decrementAndRemoveParticipation(productId, participation.getId());
                return null;
            }

            // 60초 넘게 멈춘 CANCEL_REQUESTED만 재시도 허용
            if ("CANCEL_REQUESTED".equals(st)) {
                LocalDateTime since = payment.getCancelRequestedAt();
                if (since != null && since.isAfter(LocalDateTime.now().minusSeconds(60))) {
                    throw new CustomException(ErrorCode.PAYMENT_CANCEL_IN_PROGRESS);
                }
                log.warn("[PAYMENT-AUDIT] 멈춘 CANCEL_REQUESTED 재개: paymentId={}, since={}", payment.getId(), since);
                payment.setCancelRequestedAt(LocalDateTime.now());
                paymentRepository.save(payment);
                return payment.getId();
            }

            if (!List.of("APPROVED", "DONE", "REFUND_FAILED").contains(st)) {
                throw new CustomException(ErrorCode.PARTICIPATION_NOT_FOUND);
            }

            payment.setStatus("CANCEL_REQUESTED");
            payment.setCancelRequestedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            return payment.getId();
        });

        if (refundPaymentId == null) {
            log.info("[PAYMENT-AUDIT] 참여취소(환불 없음): productId={}, buyer={}, reason={}", productId, email, reason);
            return productRepository.findById(productId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        }

        // 2. 락 밖에서 Toss 환불
        log.info("[PAYMENT-AUDIT] 참여취소 환불 시작: paymentId={}, productId={}, buyer={}, reason={}",
                refundPaymentId, productId, email, reason);
        refundViaToss(refundPaymentId, reason);

        // 3. 환불 성공 시 참여/인원 정리
        Product result = tx.execute(status -> {
            Participation participation = participationRepository
                    .findByProduct_ProductIdAndMember_Email(productId, email)
                    .orElseThrow(() -> new CustomException(ErrorCode.PARTICIPATION_NOT_FOUND));
            return decrementAndRemoveParticipation(productId, participation.getId());
        });
        log.info("[PAYMENT-AUDIT] 참여취소 환불 완료: paymentId={}, productId={}, buyer={}", refundPaymentId, productId, email);
        return result;
    }

    private Product decrementAndRemoveParticipation(Long productId, Long participationId) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        product.decrementCurrentCount();
        participationRepository.deleteById(participationId);
        return product;
    }
}
