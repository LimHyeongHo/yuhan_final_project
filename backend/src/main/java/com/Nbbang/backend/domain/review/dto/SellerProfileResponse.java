package com.Nbbang.backend.domain.review.dto;

import com.Nbbang.backend.domain.review.entity.Sentiment;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 판매자 프로필 페이지 응답 (인증 불필요, 누구나 조회).
 *
 * 지표는 저장하지 않고 조회 시점에 집계한다:
 *  - satisfactionRate : 후기 중 LIKE 비율(%)  (후기 0건이면 0)
 *  - tradeCount       : status = CLOSED_SUCCESS 인 공동구매 수
 *  - successRate      : CLOSED_SUCCESS / (CLOSED_SUCCESS + CLOSED_FAIL) (%)  (분모 0이면 null)
 *
 * reviews 는 작성자 식별정보(reviewerEmail)를 제외하고 감정/내용/작성일만 노출한다.
 */
@Getter
public class SellerProfileResponse {

    private final String sellerEmail;
    private final String nickname;
    private final LocalDateTime joinedAt;

    private final int satisfactionRate;
    private final int totalReviews;
    private final long tradeCount;
    private final Integer successRate;

    private final List<PurchaseItem> purchases;
    private final List<ReviewItem> reviews;

    private SellerProfileResponse(String sellerEmail, String nickname, LocalDateTime joinedAt,
                                  int satisfactionRate, int totalReviews, long tradeCount, Integer successRate,
                                  List<PurchaseItem> purchases, List<ReviewItem> reviews) {
        this.sellerEmail = sellerEmail;
        this.nickname = nickname;
        this.joinedAt = joinedAt;
        this.satisfactionRate = satisfactionRate;
        this.totalReviews = totalReviews;
        this.tradeCount = tradeCount;
        this.successRate = successRate;
        this.purchases = purchases;
        this.reviews = reviews;
    }

    public static SellerProfileResponse of(String sellerEmail, String nickname, LocalDateTime joinedAt,
                                           int satisfactionRate, int totalReviews, long tradeCount, Integer successRate,
                                           List<PurchaseItem> purchases, List<ReviewItem> reviews) {
        return new SellerProfileResponse(sellerEmail, nickname, joinedAt,
                satisfactionRate, totalReviews, tradeCount, successRate, purchases, reviews);
    }

    /** 판매자가 진행한 공동구매 한 건 */
    @Getter
    public static class PurchaseItem {
        private final Long productId;
        private final String title;
        private final String status;
        private final int currentCount;
        private final int targetCount;
        private final LocalDateTime deadline;

        public PurchaseItem(Long productId, String title, String status,
                            int currentCount, int targetCount, LocalDateTime deadline) {
            this.productId = productId;
            this.title = title;
            this.status = status;
            this.currentCount = currentCount;
            this.targetCount = targetCount;
            this.deadline = deadline;
        }
    }

    /** 마스킹된 후기 한 건 — 작성자 정보 없음 */
    @Getter
    public static class ReviewItem {
        private final Sentiment sentiment;
        private final String content;
        private final LocalDateTime createdAt;

        public ReviewItem(Sentiment sentiment, String content, LocalDateTime createdAt) {
            this.sentiment = sentiment;
            this.content = content;
            this.createdAt = createdAt;
        }
    }
}
