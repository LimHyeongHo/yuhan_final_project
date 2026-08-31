package com.Nbbang.backend.domain.review.service;

import com.Nbbang.backend.domain.auth.entity.UserAccount;
import com.Nbbang.backend.domain.auth.repository.UserAccountRepository;
import com.Nbbang.backend.domain.payment.repository.PaymentRepository;
import com.Nbbang.backend.domain.product.entity.Product;
import com.Nbbang.backend.domain.product.repository.ProductRepository;
import com.Nbbang.backend.domain.review.dto.EligibilityResponse;
import com.Nbbang.backend.domain.review.dto.ReviewCreateRequest;
import com.Nbbang.backend.domain.review.dto.ReviewResponse;
import com.Nbbang.backend.domain.review.dto.ReviewUpdateRequest;
import com.Nbbang.backend.domain.review.dto.SellerProfileResponse;
import com.Nbbang.backend.domain.review.entity.Review;
import com.Nbbang.backend.domain.review.entity.Sentiment;
import com.Nbbang.backend.domain.review.repository.ReviewRepository;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 거래 후기 도메인 서비스.
 *
 * - 작성 자격: 공동구매 status = CLOSED_SUCCESS + 본인 결제 status = DONE
 * - 1인 1공동구매 1후기 (DB 유니크 + 서비스 선검사)
 * - 삭제는 row 자체를 제거 → 같은 공동구매에 재작성 가능
 * - Product / Payment 는 조회만 하고 수정하지 않는다
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final String STATUS_CLOSED_SUCCESS = "CLOSED_SUCCESS";
    private static final String STATUS_CLOSED_FAIL = "CLOSED_FAIL";
    private static final String PAYMENT_STATUS_DONE = "DONE";

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final UserAccountRepository userAccountRepository;

    /* ============================= 작성 ============================= */

    @Transactional
    public ReviewResponse createReview(String reviewerEmail, ReviewCreateRequest request) {
        if (request.getProductId() == null || request.getSentiment() == null) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED);
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!isEligible(product, reviewerEmail)) {
            throw new CustomException(ErrorCode.REVIEW_NOT_ELIGIBLE);
        }
        if (reviewRepository.existsByProductIdAndReviewerEmail(product.getProductId(), reviewerEmail)) {
            throw new CustomException(ErrorCode.REVIEW_ALREADY_WRITTEN);
        }

        Review review = new Review();
        review.setProductId(product.getProductId());
        review.setReviewerEmail(reviewerEmail);
        review.setSellerEmail(product.getSellerEmail());
        review.setSentiment(request.getSentiment());
        review.setContent(normalizeContent(request.getContent()));

        Review saved = reviewRepository.save(review);
        return ReviewResponse.of(saved, product.getTitle(), true);
    }

    /* ============================= 수정 ============================= */

    @Transactional
    public ReviewResponse updateReview(String requesterEmail, Long reviewId, ReviewUpdateRequest request) {
        if (request.getSentiment() == null) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED);
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.getReviewerEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.REVIEW_NOT_OWNER);
        }

        review.setSentiment(request.getSentiment());
        review.setContent(normalizeContent(request.getContent()));
        // 더티 체킹으로 반영 (@PreUpdate 가 updatedAt 갱신)

        String productTitle = productRepository.findById(review.getProductId())
                .map(Product::getTitle)
                .orElse(null);
        return ReviewResponse.of(review, productTitle, true);
    }

    /* ============================= 삭제 ============================= */

    @Transactional
    public void deleteReview(String requesterEmail, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.getReviewerEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.REVIEW_NOT_OWNER);
        }
        reviewRepository.delete(review); // soft-delete 아님 — 재작성 가능해야 함
    }

    /* ========================= 작성 자격 확인 ========================= */

    @Transactional(readOnly = true)
    public EligibilityResponse checkEligibility(String reviewerEmail, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // 이미 쓴 후기가 있으면 수정/삭제 동선으로 넘어가야 하므로 먼저 확인
        Review existing = reviewRepository
                .findByProductIdAndReviewerEmail(product.getProductId(), reviewerEmail)
                .orElse(null);
        if (existing != null) {
            return EligibilityResponse.alreadyWritten(ReviewResponse.of(existing, product.getTitle(), true));
        }

        if (product.getSellerEmail() == null) {
            return EligibilityResponse.ineligible("판매자 정보가 확인되지 않는 공동구매입니다");
        }
        if (!STATUS_CLOSED_SUCCESS.equals(product.getStatus())) {
            return EligibilityResponse.ineligible("정상 종료된 공동구매가 아닙니다");
        }
        if (!paidDone(product.getProductId(), reviewerEmail)) {
            return EligibilityResponse.ineligible("결제 완료된 참여 내역이 없습니다");
        }
        return EligibilityResponse.eligible();
    }

    /** POST 작성 시 사용하는 자격 판정 (status + 결제완료). 중복 여부는 별도 검사. */
    private boolean isEligible(Product product, String reviewerEmail) {
        return STATUS_CLOSED_SUCCESS.equals(product.getStatus())
                && paidDone(product.getProductId(), reviewerEmail);
    }

    private boolean paidDone(Long productId, String email) {
        // [의존] 건우님 PaymentRepository 에 추가 예정인 메서드.
        //  boolean existsByProductIdAndMember_EmailAndStatus(Long productId, String memberEmail, String status)
        return paymentRepository.existsByProductIdAndMember_EmailAndStatus(productId, email, PAYMENT_STATUS_DONE);
    }

    /* ======================= 판매자 프로필 ======================= */

    @Transactional(readOnly = true)
    public SellerProfileResponse getSellerProfile(String sellerEmail) {
        UserAccount seller = userAccountRepository.findById(sellerEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        List<Product> products = productRepository.findBySellerEmailOrderByCreatedAtDesc(sellerEmail);
        long closedSuccess = products.stream().filter(p -> STATUS_CLOSED_SUCCESS.equals(p.getStatus())).count();
        long closedFail = products.stream().filter(p -> STATUS_CLOSED_FAIL.equals(p.getStatus())).count();
        Integer successRate = (closedSuccess + closedFail) == 0
                ? null
                : (int) Math.round(closedSuccess * 100.0 / (closedSuccess + closedFail));

        List<Review> reviews = reviewRepository.findBySellerEmailOrderByCreatedAtDesc(sellerEmail);
        int total = reviews.size();
        // 가중 만족도: 좋아요 100 / 보통이에요 50 / 싫어요 0 → 전체 평균
        long likeCount = reviews.stream().filter(r -> r.getSentiment() == Sentiment.LIKE).count();
        long sosoCount = reviews.stream().filter(r -> r.getSentiment() == Sentiment.SOSO).count();
        int satisfactionRate = total == 0
                ? 0
                : (int) Math.round((likeCount * 100.0 + sosoCount * 50.0) / total);

        List<SellerProfileResponse.PurchaseItem> purchases = products.stream()
                .map(p -> new SellerProfileResponse.PurchaseItem(
                        p.getProductId(), p.getTitle(), p.getStatus(),
                        p.getCurrentCount() == null ? 0 : p.getCurrentCount(),
                        p.getTargetCount() == null ? 0 : p.getTargetCount(),
                        p.getDeadline()))
                .toList();

        List<SellerProfileResponse.ReviewItem> reviewItems = reviews.stream()
                .map(r -> new SellerProfileResponse.ReviewItem(r.getSentiment(), r.getContent(), r.getCreatedAt()))
                .toList();

        return SellerProfileResponse.of(
                sellerEmail, seller.getNickname(), seller.getCreatedAt(),
                satisfactionRate, total, closedSuccess, successRate,
                purchases, reviewItems);
    }

    /* ============================= 유틸 ============================= */

    private String normalizeContent(String content) {
        if (content == null) {
            return null;
        }
        String trimmed = content.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
