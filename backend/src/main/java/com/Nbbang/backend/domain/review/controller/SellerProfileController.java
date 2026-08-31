package com.Nbbang.backend.domain.review.controller;

import com.Nbbang.backend.domain.review.dto.SellerProfileResponse;
import com.Nbbang.backend.domain.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 판매자 프로필 API (인증 불필요)
 *
 * GET /api/sellers/{email}/profile
 *   → 거래 만족도 %, 거래 횟수, 정상 종료율, 진행한 공동구매 목록, 익명 후기 목록
 */
@RestController
@RequestMapping("/api/sellers")
@RequiredArgsConstructor
public class SellerProfileController {

    private final ReviewService reviewService;

    @GetMapping("/{email}/profile")
    public ResponseEntity<SellerProfileResponse> profile(@PathVariable String email) {
        return ResponseEntity.ok(reviewService.getSellerProfile(email));
    }
}
