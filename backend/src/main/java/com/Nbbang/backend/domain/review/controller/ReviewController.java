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

/**
 * 거래 후기 REST API
 *
 * POST   /api/reviews                       후기 작성
 * PUT    /api/reviews/{id}                   후기 수정 (작성자 본인만)
 * DELETE /api/reviews/{id}                   후기 삭제 (작성자 본인만, row 제거)
 * GET    /api/reviews/eligibility?productId= 해당 공동구매 후기 작성 자격 조회
 */
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

    // 세션에서 로그인 이메일 추출 (domain/chat 의 getEmail 패턴 재사용)
    private String getEmail(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        return userId;
    }
}
