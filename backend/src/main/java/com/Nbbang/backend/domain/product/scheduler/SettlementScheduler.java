package com.Nbbang.backend.domain.product.scheduler;

import com.Nbbang.backend.domain.product.entity.Product;
import com.Nbbang.backend.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementScheduler {

    private final ProductRepository productRepository;

    // 매 1분마다 실행 (밀리초 단위, 60000 = 1분)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void processExpiredProducts() {
        LocalDateTime now = LocalDateTime.now();
        List<Product> expiredProducts = productRepository.findByDeadlineBeforeAndStatus(now, "OPEN");

        if (expiredProducts.isEmpty()) {
            log.info("마감 처리할 공동구매 상품이 없습니다.");
            return;
        }

        log.info("마감 기한이 지난 상품 {}건에 대해 상태 업데이트를 시작합니다.", expiredProducts.size());

        for (Product product : expiredProducts) {
            // targetCount 이상의 인원이 모였으면 성공(CLOSED_SUCCESS), 아니면 실패(CLOSED_FAIL)
            if (product.getCurrentCount() >= product.getTargetCount()) {
                product.setStatus("CLOSED_SUCCESS");
                log.info("상품 [ID:{}] 모집 성공 (목표: {}, 현재: {})", product.getProductId(), product.getTargetCount(), product.getCurrentCount());
            } else {
                product.setStatus("CLOSED_FAIL");
                log.info("상품 [ID:{}] 모집 실패 (목표: {}, 현재: {})", product.getProductId(), product.getTargetCount(), product.getCurrentCount());
            }
        }
    }
}
