package com.Nbbang.backend.domain.review.controller;

import com.Nbbang.backend.domain.review.dto.SellerProfileResponse;
import com.Nbbang.backend.domain.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 판매자 프로필 조회 API (로그인 없이 누구나 볼 수 있음)
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
