package com.Nbbang.backend.domain.bookreport.service;

import com.Nbbang.backend.domain.bookreport.analysis.BookReportAnalysisResult;
import com.Nbbang.backend.domain.bookreport.entity.BookDifficulty;
import com.Nbbang.backend.domain.product.entity.ProductCategory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookReportOutputGuardTest {

    private final BookReportOutputGuard guard = new BookReportOutputGuard();

    @Test
    void rejectsUnsupportedSalesClaims() {
        BookReportAnalysisResult result = new BookReportAnalysisResult(
                ProductCategory.COMPUTER_SOFTWARE, 90, BookDifficulty.BEGINNER,
                List.of("프로그래밍"), "베스트셀러 도서입니다.", "입문자", "소개 기반 추천",
                List.of("contents"), 0.9, true, "test", "test-v1");

        assertThatThrownBy(() -> guard.validate(ProductCategory.COMPUTER_SOFTWARE, result))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
