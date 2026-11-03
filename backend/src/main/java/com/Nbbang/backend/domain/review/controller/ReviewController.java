package com.Nbbang.backend.domain.review.controller;

import com.Nbbang.backend.domain.review.dto.EligibilityResponse;
import com.Nbbang.backend.domain.review.dto.ReviewCreateRequest;
import com.Nbbang.backend.domain.review.dto.ReviewResponse;
import com.Nbbang.backend.domain.review.dto.ReviewUpdateRequest;
import com.Nbbang.backend.domain.review.service.ReviewService;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 거래 후기 작성·수정·삭제와 작성 자격 조회 API
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> create(@RequestBody ReviewCreateRequest request, HttpSession session) {
        return ResponseEntity.ok(reviewService.createReview(getEmail(session), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> update(@PathVariable Long id,
                                                 @RequestBody ReviewUpdateRequest request,
                                                 HttpSession session) {
        return ResponseEntity.ok(reviewService.updateReview(getEmail(session), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpSession session) {
        reviewService.deleteReview(getEmail(session), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/eligibility")
    public ResponseEntity<EligibilityResponse> eligibility(@RequestParam Long productId, HttpSession session) {
        return ResponseEntity.ok(reviewService.checkEligibility(getEmail(session), productId));
    }

    // 세션에서 로그인 이메일을 꺼냄
    private String getEmail(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        return userId;
    }
}
