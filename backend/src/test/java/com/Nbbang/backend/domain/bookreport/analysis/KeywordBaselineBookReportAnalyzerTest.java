package com.Nbbang.backend.domain.bookreport.analysis;

import com.Nbbang.backend.domain.bookreport.entity.BookDifficulty;
import com.Nbbang.backend.domain.product.entity.ProductCategory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordBaselineBookReportAnalyzerTest {

    private final KeywordBaselineBookReportAnalyzer analyzer = new KeywordBaselineBookReportAnalyzer();

    @Test
    void producesGroundedStructuredOutputFromMatchedKeywords() {
        BookReportAnalysisResult result = analyzer.analyze(new BookReportAnalysisRequest(
                ProductCategory.COMPUTER_SOFTWARE,
                "자바 프로그래밍 입문",
                List.of("홍길동"),
                "유한출판",
                "프로그래밍 기초를 설명합니다.",
                List.of("프로그래밍", "자료구조")
        ));

        assertThat(result.category()).isEqualTo(ProductCategory.COMPUTER_SOFTWARE);
        assertThat(result.difficulty()).isEqualTo(BookDifficulty.BEGINNER);
        assertThat(result.relevanceScore()).isBetween(0, 100);
        assertThat(result.confidence()).isGreaterThanOrEqualTo(0.70);
        assertThat(result.shouldInclude()).isTrue();
        assertThat(result.evidenceFields()).containsExactly("title", "contents", "matchedKeywords");
    }
}
