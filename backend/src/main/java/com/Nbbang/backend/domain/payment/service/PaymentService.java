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
    private final PlatformTransactionManager transactionManager; // [신규] 결제 취소/자동환불 saga용

    @Value("${toss.secret-key}")
    private String secretKey;

    // [NFR-004] 외부 결제사(Toss) 호출에 명시적 타임아웃. 미설정 시 응답이 없으면 호출 스레드가
    // 무한 대기해 결제/환불 요청이 밀린다. connect 3초 / response 10초로 제한하고, 초과 시
    // WebClient가 예외를 던져 confirmPayment·callTossCancel의 catch에서 실패로 처리된다.
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.tosspayments.com")
            .clientConnector(new ReactorClientHttpConnector(
                    HttpClient.create()
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                            .responseTimeout(Duration.ofSeconds(10))))
            .build();

    // 결제 준비: 결제창을 열기 전에 서버가 실제 상품 가격을 확인하고 PENDING 상태로 기록해둔다.
    // userId는 로그인 세션에서 검증된 값만 들어와야 함 (PaymentController에서 보장).
    // [수정] PRD-RQ-003: 정원 체크를 비관적 락으로 직렬화
    @Transactional
    public PaymentPrepareResponse prepare(PaymentPrepareRequest request, String userId) {
        Product product = productRepository.findByIdForUpdate(request.getProductId())
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

    // [신규] PAY-RQ-001: orderId로 Toss에 결제 실제 상태를 조회한다. 승인/취소 요청의 응답이 유실됐을 때
    // "실제로 처리됐는지"를 대조하는 용도. 해당 주문이 Toss에 아직 없으면(승인 시도 전) null 반환.
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
                return null; // Toss에 아직 존재하지 않는 주문
            }
            log.error("Toss 결제조회 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.PAYMENT_LOOKUP_FAILED);
        } catch (Exception e) {
            log.error("Toss 결제조회 중 알 수 없는 오류", e);
            throw new CustomException(ErrorCode.PAYMENT_LOOKUP_FAILED);
        }
    }

    // [신규] PAY-RQ-001 Edge1: confirm 호출의 응답만 유실된 채 실패했을 수 있으므로, 실패 시 Toss에
    // 실제 상태를 물어본다. 이미 DONE(+금액 일치)이면 그 결과로 진행 — confirm이 사실상 멱등해진다.
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

    // Toss 리다이렉트 콜백 처리: PENDING -> (Toss 승인) APPROVED -> (참여 확정) DONE.
    //
    // [수정] PRD-RQ-003: 예전엔 이 메서드가 하나의 트랜잭션이라 Toss 승인(실제 인출) 후 joinProduct가
    // 실패하면 status=DONE 저장까지 롤백돼 돈만 빠져나가고 기록이 안 남았다.
    // [수정] PAY-RQ-001 §3.1: 예전엔 승인 직후 바로 DONE을 커밋해서, "DONE 커밋"과 "참여 확정" 사이에
    // 프로세스가 죽으면 재진입 콜백이 DONE만 보고 성공 반환 -> 참여 생성도 환불도 안 일어났다.
    // 이제 승인 직후엔 APPROVED만 커밋하고 참여 확정 후에만 DONE으로 바꾼다. 콜백이 끊겨도
    // 재진입 시 현재 상태를 보고 끊긴 지점부터 재개한다.
    public PaymentResponse processSuccessCallback(String orderId, String paymentKey, Long amount) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        String phase = tx.execute(status -> {
            Payment payment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND));
            switch (payment.getStatus()) {
                case "DONE":
                    return "DONE";
                case "CANCELED": // 참여 확정 실패로 이미 전액 환불됨 - 성공으로 응답하면 안 됨
                    throw new CustomException(ErrorCode.PAYMENT_JOIN_FAILED);
                case "CANCEL_REQUESTED":
                case "REFUND_FAILED": // 환불이 시작됐으나 미완료 - 환불부터 재개
                    return "RESUME_REFUND";
                case "APPROVED": // 승인은 끝났고 참여 확정만 남음
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
            refundViaToss(paymentId, "참여 확정 실패 자동 환불(재개)"); // 멱등
            throw new CustomException(ErrorCode.PAYMENT_JOIN_FAILED);
        }

        // confirm 응답이 유실된 채 죽었어도 재진입 시 PENDING이라 여기로 온다. confirmOrReconcile이
        // 실패 시 Toss 결제조회로 실제 승인 여부를 대조하므로 confirm은 사실상 멱등하다. (PAY-RQ-001 Edge1)
        String method = null; // 재개 경로에선 알 수 없음
        if ("CONFIRM".equals(phase)) {
            PaymentRequest request = new PaymentRequest();
            request.setPaymentKey(paymentKey);
            request.setOrderId(orderId);
            request.setAmount(amount);
            PaymentResponse confirmed = confirmOrReconcile(request, amount); // 트랜잭션 밖, 실제 인출
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

        // 참여가 이미 있으면(DONE 전환 직전에 죽은 경우) 재조인하면 ALREADY_JOINED로 실패해 정상 결제를 오환불하게 됨
        boolean alreadyJoined = participationRepository
                .existsByProduct_ProductIdAndMember_Email(productId, buyerEmail);

        if (!alreadyJoined) {
            try {
                productService.joinProduct(productId, buyerEmail);
            } catch (CustomException e) {
                log.warn("결제는 승인됐지만 참여 확정 실패, 자동 환불: orderId={}, reason={}", orderId, e.getErrorCode());
                refundViaToss(paymentId, "참여 확정 실패 자동 환불");
                throw new CustomException(ErrorCode.PAYMENT_JOIN_FAILED);
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
    //
    // [수정] PAY-RQ-001 Edge2: Toss 취소 호출의 응답만 유실된 채 워커가 죽으면, 재시도가 취소를 또
    // 호출해 Toss가 "이미 취소됨"으로 거부 -> REFUND_FAILED 오판이 났다. 이제 취소 호출 전에 Toss
    // 실제 상태를 조회해서, 이미 (부분)취소돼 있으면 로컬 상태만 CANCELED로 맞추고 끝낸다.
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

        // Toss에 이미 취소돼 있으면 재호출하지 않고 로컬만 정리 (중복 취소 -> REFUND_FAILED 오판 방지)
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

    // [신규] PRD-RQ-001 + PAY-RQ-001: 구매자 본인의 참여 취소.
    // 결제 완료 건이 있으면 Toss 환불이 성공한 뒤에만 Participation/currentCount를 확정적으로
    // 정리한다 (환불 실패 시 인원/참여는 그대로 유지).
    //
    // [수정] PRD-RQ-001: 예전엔 participation 조회 -> 상태 판정 -> 환불이 별도 단계라, 같은 사용자가
    // 취소를 연타하면 두 요청이 모두 상태 판정을 통과해 Toss 환불을 이중 호출할 수 있었다. 이제
    // "환불 대상 판정 + CANCEL_REQUESTED 선점"을 Product 락 안의 한 트랜잭션에 묶는다. 먼저 들어온
    // 요청이 CANCEL_REQUESTED로 바꾸면 뒤이은 요청은 그 상태를 보고 PAYMENT_CANCEL_IN_PROGRESS로
    // 거절되므로 환불은 한 번만 실행된다. (Edge2) 단, 60초 넘게 그 상태로 멈춰 있으면 워커가 죽은
    // 것으로 보고 재시도가 Toss 실제 상태를 대조해 환불을 재개한다.
    public Product cancelParticipation(Long productId, String email, String reason) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        // Phase 1: Product 락 안에서 취소 대상 확정. 환불 불필요 케이스는 여기서 인원 정리까지 끝내고,
        // 환불 대상이면 CANCEL_REQUESTED로 선점한 뒤 결제 id를 돌려준다(null이면 이미 완료).
        Long refundPaymentId = tx.execute(status -> {
            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
            if (!"OPEN".equals(product.getStatus())) {
                throw new CustomException(ErrorCode.PRODUCT_CANNOT_MODIFY_COMPLETED);
            }

            Participation participation = participationRepository
                    .findByProduct_ProductIdAndMember_Email(productId, email)
                    .orElseThrow(() -> new CustomException(ErrorCode.PARTICIPATION_NOT_FOUND));

            Payment payment = paymentRepository
                    .findFirstByProductIdAndMember_EmailOrderByIdDesc(productId, email)
                    .orElse(null);
            String st = payment == null ? null : payment.getStatus();

            // 결제 기록 없는 참여(레거시 무료 참여) / 이미 취소 완료 / 아직 인출 전(PENDING) — 인원만 정리
            if (payment == null || "CANCELED".equals(st) || "PENDING".equals(st)) {
                decrementAndRemoveParticipation(productId, participation.getId());
                return null;
            }

            // [PAY-RQ-001 Edge2] CANCEL_REQUESTED: 최근에 선점된 건이면 다른 요청이 환불 진행 중이므로
            // 막는다(이중 환불 방지). 60초 넘게 멈춰 있으면 워커가 죽은 것으로 보고 재선점 —
            // 아래 refundViaToss가 Toss 실제 상태를 대조해 안전하게 재개한다.
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

            // APPROVED = 인출됐으나 DONE 전환 직전 콜백이 끊긴 건, REFUND_FAILED = 이전 환불 재시도
            if (!List.of("APPROVED", "DONE", "REFUND_FAILED").contains(st)) {
                throw new CustomException(ErrorCode.PARTICIPATION_NOT_FOUND);
            }

            payment.setStatus("CANCEL_REQUESTED"); // 선점 — 이 커밋 이후 동시 요청은 위에서 막힌다
            payment.setCancelRequestedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            return payment.getId();
        });

        if (refundPaymentId == null) {
            log.info("[PAYMENT-AUDIT] 참여취소(환불 없음): productId={}, buyer={}, reason={}", productId, email, reason);
            return productRepository.findById(productId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        }

        // Phase 2: 락 밖에서 Toss 환불(네트워크). 실패 시 refundViaToss가 REFUND_FAILED로 남기고
        // PAYMENT_REFUND_FAILED를 던지며 여기서 중단 — Participation/currentCount는 그대로 유지된다.
        log.info("[PAYMENT-AUDIT] 참여취소 환불 시작: paymentId={}, productId={}, buyer={}, reason={}",
                refundPaymentId, productId, email, reason);
        refundViaToss(refundPaymentId, reason);

        // Phase 3: 환불 성공 -> 인원/참여 정리
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
