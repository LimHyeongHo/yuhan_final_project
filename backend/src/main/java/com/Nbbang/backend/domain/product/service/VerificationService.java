package com.Nbbang.backend.domain.product.service;

import com.Nbbang.backend.domain.product.entity.Product;
import com.Nbbang.backend.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final ProductRepository productRepository;
    private final BlockchainService blockchainService;
    private final AladdinApiService aladdinApiService;

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

        // 1. 블록체인에서 원본 해시 조회 (비동기로 등록되므로 아직 없을 수도 있음)
        String blockchainHash = blockchainService.getHash(productId);
        if (blockchainHash == null || blockchainHash.isEmpty()) {
            result.put("status", "PENDING");
            result.put("message", "블록체인에 아직 기록되지 않았거나 조회할 수 없습니다.");
            return result;
        }

        // 2. 현재 DB 데이터를 기반으로 해시 생성 (ProductID + ISBN + Price)
        String currentDataString = productId + "_" + (product.getIsbn() != null ? product.getIsbn() : "") + "_" + product.getPrice();
        String currentHash = hashString(currentDataString);

        String cleanCurrent = currentHash.replace("0x", "").trim().toLowerCase().replaceAll("[^a-f0-9]", "");
        String cleanBc = blockchainHash.replace("0x", "").trim().toLowerCase().replaceAll("[^a-f0-9]", "");

        // 3. 해시 비교 (FORGED 판별)
        if (!cleanCurrent.equals(cleanBc)) {
            result.put("status", "FORGED");
            result.put("message", "데이터 위변조가 감지되었습니다. (DB: " + cleanCurrent.substring(0,6) + " != BC: " + cleanBc.substring(0, Math.min(6, cleanBc.length())) + ")");
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
        return result;
    }

    /**
     * SHA-256 해시 생성 헬퍼 메서드
     */
    public String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException("해시 생성 중 오류 발생", ex);
        }
    }
}
