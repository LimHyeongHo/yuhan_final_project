package com.Nbbang.backend.domain.search.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record KakaoBookSnapshot(
        String title,
        List<String> authors,
        String publisher,
        String isbn13,
        String isbn10,
        String contents,
        String sourceUrl,
        LocalDate publishedAt,
        BigDecimal price,
        BigDecimal salePrice,
        String saleStatus
) {
    public KakaoBookSnapshot {
        authors = authors == null ? List.of() : List.copyOf(authors);
    }
}
