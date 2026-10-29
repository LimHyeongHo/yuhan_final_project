package com.Nbbang.backend.domain.payment.scheduler;

import com.Nbbang.backend.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

// 상품 삭제/모집 실패/판매자 탈퇴로 끝난 공구 결제 자동 환불 → PaymentService.refundOrphanedPayment()
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRefundScheduler {

    private final PaymentService paymentService;

    // 이전 실행 종료 후 1분 간격
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
                log.error("[PAYMENT-AUDIT] 자동 환불 처리 중 오류: paymentId={}, type={}",
                        paymentId, e.getClass().getSimpleName());
            }
        }
    }
}
