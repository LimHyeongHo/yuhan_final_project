package com.Nbbang.backend.domain.review.dto;

import com.Nbbang.backend.domain.review.entity.Review;
import com.Nbbang.backend.domain.review.entity.Sentiment;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 후기 작성/수정 후 프론트로 돌려주는 응답.
 * 작성자 본인에게만 반환되므로 mine 은 항상 true 지만, 명시적으로 내려준다.
 */
@Getter
public class ReviewResponse {

    private final Long id;
    private final Long productId;
    private final Sentiment sentiment;
    private final String content;
    private final String productTitle;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final boolean mine;

    private ReviewResponse(Long id, Long productId, Sentiment sentiment, String content,
                           String productTitle, LocalDateTime createdAt, LocalDateTime updatedAt, boolean mine) {
        this.id = id;
        this.productId = productId;
        this.sentiment = sentiment;
        this.content = content;
        this.productTitle = productTitle;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.mine = mine;
    }

    public static ReviewResponse of(Review review, String productTitle, boolean mine) {
        return new ReviewResponse(
                review.getId(),
                review.getProductId(),
                review.getSentiment(),
                review.getContent(),
                productTitle,
                review.getCreatedAt(),
                review.getUpdatedAt(),
                mine
        );
    }
}
