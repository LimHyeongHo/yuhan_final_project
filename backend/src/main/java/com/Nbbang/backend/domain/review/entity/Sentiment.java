package com.Nbbang.backend.domain.review.entity;

/**
 * 거래 후기 3단계 평가.
 * LIKE(좋아요) / SOSO(보통이에요) / DISLIKE(싫어요)
 * 판매자 프로필의 "거래 만족도 %"는 LIKE 비율로 집계한다.
 */
public enum Sentiment {
    LIKE, SOSO, DISLIKE
}
