package com.Nbbang.backend.domain.review.dto;

import com.Nbbang.backend.domain.review.entity.Sentiment;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewUpdateRequest {
    private Sentiment sentiment;
    private String content;
}
