package com.Nbbang.backend.domain.review.dto;

import com.Nbbang.backend.domain.review.entity.Sentiment;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

// 판매자 프로필 응답 (지표는 저장하지 않고 조회할 때마다 계산함)
// 만족도는 좋아요 100·보통 50·싫어요 0점 평균, 성공률은 성공 / (성공 + 실패)
// 후기는 작성자 정보 없이 평가·내용·작성일만 보여줌
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

    // 판매자가 진행한 공동구매 한 건
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

    // 후기 한 건 (작성자 정보 없음)
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
