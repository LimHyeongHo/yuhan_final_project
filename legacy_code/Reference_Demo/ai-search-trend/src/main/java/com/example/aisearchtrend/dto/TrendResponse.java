package com.example.aisearchtrend.dto;

import lombok.Data;
import java.util.List;

@Data
public class TrendResponse {
    private List<String> trendingBooks;    // 인기 급상승
    private List<String> recommendedBooks; // AI 추천 도서 추가
    private String analysis;              // 트렌드 요약
    private String generatedAt;           // 생성 시간
}