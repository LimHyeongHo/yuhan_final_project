package com.example.aisearchtrend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SearchLog {
    private String keyword;     // 검색어
    private int searchCount;    // 검색 횟수
    private LocalDateTime lastSearched; // 마지막 검색 시간
}