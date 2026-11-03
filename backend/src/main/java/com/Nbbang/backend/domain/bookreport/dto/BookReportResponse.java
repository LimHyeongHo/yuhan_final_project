package com.Nbbang.backend.domain.bookreport.dto;

import com.Nbbang.backend.domain.bookreport.entity.BookDifficulty;
import com.Nbbang.backend.domain.bookreport.entity.BookReportStatus;
import com.Nbbang.backend.domain.product.entity.ProductCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record BookReportResponse(
        LocalDate reportDate,
        ProductCategory category,
        String categoryName,
        BookReportStatus status,
        LocalDateTime generatedAt,
        Source source,
        List<Section> sections,
        String notice
) {
    public record Source(
            String provider,
            LocalDateTime fetchedAt,
            String keywordConfigVersion
    ) {
    }

    public record Section(
            String type,
            String title,
            List<Book> books
    ) {
    }

    public record Book(
            Long analysisId,
            String title,
            List<String> authors,
            String publisher,
            LocalDate publishedAt,
            String isbn,
            BigDecimal price,
            BigDecimal salePrice,
            int discountRate,
            int relevanceScore,
            int attentionScore,
            BookDifficulty difficulty,
            List<String> topics,
            String summary,
            String recommendedFor,
            String recommendationReason,
            String sourceUrl,
            LocalDateTime sourceFetchedAt
    ) {
    }
}
