package com.Nbbang.backend.domain.review.dto;

import com.Nbbang.backend.domain.review.entity.Sentiment;
import lombok.Getter;
import lombok.Setter;

/**
 * 후기 수정 요청 body.
 * { "sentiment": "SOSO", "content": "수정된 한 줄 후기(선택)" }
 */
@Getter
@Setter
public class ReviewUpdateRequest {
    private Sentiment sentiment;
    private String content;
}
