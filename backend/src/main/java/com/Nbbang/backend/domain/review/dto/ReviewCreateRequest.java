package com.Nbbang.backend.domain.review.dto;

import com.Nbbang.backend.domain.review.entity.Sentiment;
import lombok.Getter;
import lombok.Setter;

/**
 * 후기 작성 요청 body.
 * { "productId": 1, "sentiment": "LIKE", "content": "한 줄 후기(선택)" }
 */
@Getter
@Setter
public class ReviewCreateRequest {
    private Long productId;
    private Sentiment sentiment;
    private String content;
}
