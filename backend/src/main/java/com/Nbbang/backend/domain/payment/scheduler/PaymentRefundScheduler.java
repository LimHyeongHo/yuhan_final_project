package com.Nbbang.backend.domain.payment.scheduler;

import com.Nbbang.backend.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

// 상품 삭제/모집 실패/판매자 탈퇴로 끝난 공구의 결제를 자동 환불한다.
// 그 처리들은 다른 도메인(product/admin/member)에 있고 환불 단계가 없어서, 결제 쪽에서 주기적으로 정리한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRefundScheduler {

    private final PaymentService paymentService;

    // 이전 실행이 끝난 뒤 1분 간격 (Toss 호출이 길어져도 실행이 겹치지 않도록 fixedDelay)
    @Scheduled(fixedDelay = 60000)
    public void refundOrphanedPayments() {
        List<Long> paymentIds = paymentService.findOrphanedPaymentIds();
        if (paymentIds.isEmpty()) {
            return;
        }

        log.info("[PAYMENT-AUDIT] 종료된 공구의 미환불 결제 {}건 자동 환불 처리", paymentIds.size());
        for (Long paymentId : paymentIds) {
            try {
                paymentService.refundOrphanedPayment(paymentId);
            } catch (Exception e) {
                // 한 건 실패가 나머지 처리를 막지 않도록 건별로 격리
                log.error("[PAYMENT-AUDIT] 자동 환불 처리 중 오류: paymentId={}, type={}",
                        paymentId, e.getClass().getSimpleName());
            }
        }
    }
}
