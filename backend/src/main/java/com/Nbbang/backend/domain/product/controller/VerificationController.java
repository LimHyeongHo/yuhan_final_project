package com.Nbbang.backend.domain.product.controller;

import com.Nbbang.backend.domain.product.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    /**
     * 특정 상품의 블록체인 및 알라딘 정가 이중 교차 검증을 수행합니다.
     * @param id 상품 ID
     * @return 검증 상태 (VALID, GOOD_DEAL, ANCHORING_WARNING, FORGED, PENDING, ERROR)
     */
    @GetMapping("/{id}/verify")
    public ResponseEntity<Map<String, Object>> verifyProduct(@PathVariable Long id) {
        Map<String, Object> result = verificationService.verifyProduct(id);
        return ResponseEntity.ok(result);
    }
}
