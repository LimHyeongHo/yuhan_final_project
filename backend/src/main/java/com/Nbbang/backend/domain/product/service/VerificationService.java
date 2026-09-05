package com.Nbbang.backend.domain.product.service;

import com.Nbbang.backend.domain.product.entity.Product;
import com.Nbbang.backend.domain.product.entity.BlockchainJobStatus;
import com.Nbbang.backend.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.Nbbang.backend.domain.log.entity.SystemLog;
import com.Nbbang.backend.domain.log.repository.SystemLogRepository;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final ProductRepository productRepository;
    private final BlockchainService blockchainService;
    private final ProductHashService productHashService;
    private final AladdinApiService aladdinApiService;
    private final SystemLogRepository systemLogRepository;

    /**
     * 상품 교차 검증 수행
     * @param productId 검증할 상품 ID
     * @return 검증 결과 Status 와 추가 데이터
     */
    public Map<String, Object> verifyProduct(Long productId) {
        Map<String, Object> result = new HashMap<>();
        
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            result.put("status", "ERROR");
            result.put("message", "상품을 찾을 수 없습니다.");
            return result;
        }

        // DB 작업 상태를 먼저 반환해 txHash 부재 원인과 재시도 이력을 API에서 구분한다.
        result.put("txHash", product.getTxHash());
        result.put("blockchainStatus", product.getBlockchainStatus());
        result.put("retryCount", product.getBlockchainRetryCount());
        result.put("lastError", product.getBlockchainLastError());

        // 1. 블록체인 조회 장애와 아직 기록되지 않은 상태를 구분한다.
        BlockchainService.BlockchainReadResult readResult = blockchainService.readHash(productId);
        if (!readResult.success()) {
            if (product.getBlockchainStatus() == BlockchainJobStatus.FAILED_FINAL) {
                result.put("status", "FAILED");
                result.put("message", product.getBlockchainLastError() != null
                        ? product.getBlockchainLastError()
                        : "블록체인 기록이 최종 실패했습니다.");
            } else if ("UNAVAILABLE".equals(readResult.code())) {
                result.put("status", "UNAVAILABLE");
                result.put("message", "블록체인 검증 서비스를 일시적으로 사용할 수 없습니다.");
            } else {
                result.put("status", "PENDING");
                result.put("message", "블록체인 기록을 처리하고 있습니다.");
            }
            result.put("retryable", product.getBlockchainStatus() != BlockchainJobStatus.FAILED_FINAL);
            return result;
        }

        String blockchainHash = readResult.hash();
        if (blockchainHash == null || blockchainHash.isEmpty()) {
            result.put("status", "PENDING");
            result.put("message", "블록체인 기록을 처리하고 있습니다.");
            result.put("retryable", true);
            return result;
        }

        // 2. 현재 DB 데이터를 기반으로 해시 생성 (ProductID + ISBN + Price)
        String currentDataString = productHashService.buildDataString(product);
        String currentHash = productHashService.calculateHash(product);

        // 스마트 영수증(보증서)용 상세 데이터 추가
        result.put("blockchainHash", blockchainHash);
        result.put("dbHash", currentHash);
        result.put("targetData", currentDataString);

        String cleanCurrent = currentHash.replace("0x", "").trim().toLowerCase().replaceAll("[^a-f0-9]", "");
        String cleanBc = blockchainHash.replace("0x", "").trim().toLowerCase().replaceAll("[^a-f0-9]", "");

        // 3. 해시 비교 (FORGED 판별)
        if (!cleanCurrent.equals(cleanBc)) {
            result.put("status", "FORGED");
            result.put("message", "데이터 위변조가 감지되었습니다. (DB: " + cleanCurrent.substring(0,6) + " != BC: " + cleanBc.substring(0, Math.min(6, cleanBc.length())) + ")");
            
            // 보안 로그 기록 (TAMPERED)
            systemLogRepository.save(SystemLog.builder()
                    .displayId("TX-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                    .type("SECURITY")
                    .status("TAMPERED")
                    .diff("Diff: DB Hash Mismatch")
                    .detail("위변조 추적")
                    .build());
            
            return result;
        }

        // 4. 알라딘 공식 정가와 비교 (ANCHORING_WARNING, GOOD_DEAL 판별)
        BigDecimal aladdinPrice = product.getAladdinPrice();
        
        // 캐싱된 정가가 없다면 API 호출
        if (aladdinPrice == null && product.getIsbn() != null) {
            aladdinPrice = aladdinApiService.fetchAladdinPrice(product.getIsbn());
            if (aladdinPrice != null) {
                // DB에 정가 캐싱 저장
                product.setAladdinPrice(aladdinPrice);
                productRepository.save(product);
            }
        }
        
        // API 호출 실패 등으로 여전히 null일 경우, DB에 저장된 originalPrice로 대체 (API Block 방어)
        if (aladdinPrice == null) {
            aladdinPrice = product.getOriginalPrice();
        }

        if (aladdinPrice != null) {
            result.put("aladdinPrice", aladdinPrice);
            double currentPrice = product.getPrice().doubleValue();
            double officialPrice = aladdinPrice.doubleValue();

            if (currentPrice > officialPrice * 1.1) {
                result.put("status", "ANCHORING_WARNING");
                result.put("message", "공식 정가(10% 초과) 대비 비정상적으로 높게 등록된 가격입니다. 시세 조작에 주의하세요.");
                return result;
            } else if (currentPrice <= officialPrice * 0.8) {
                result.put("status", "GOOD_DEAL");
                result.put("message", "평균 금액보다 가격이 저렴합니다!");
                return result;
            }
        }

        // 해시가 일치하고 가격이 범위 내에 있으면 정상
        result.put("status", "VALID");
        result.put("message", "정상적으로 검증되었습니다.");
        
        // 보안 로그 기록 (SUCCESS)
        systemLogRepository.save(SystemLog.builder()
                .displayId("TX-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .type("SECURITY")
                .status("SUCCESS")
                .diff("0x0000...0000")
                .detail("상세 정보")
                .build());
                
        return result;
    }

}
