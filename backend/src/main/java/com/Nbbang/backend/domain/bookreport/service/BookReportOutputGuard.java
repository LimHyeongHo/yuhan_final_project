package com.Nbbang.backend.domain.bookreport.service;

import com.Nbbang.backend.domain.bookreport.analysis.BookReportAnalysisResult;
import com.Nbbang.backend.domain.product.entity.ProductCategory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookReportOutputGuard {

    private static final List<String> FORBIDDEN_EXPRESSIONS = List.of(
            "베스트셀러", "판매 순위", "가장 많이 팔린", "판매량", "수요 보장",
            "학습 효과 보장", "강의 채택", "수상 경력", "독자 평가"
    );

    public void validate(ProductCategory requestedCategory, BookReportAnalysisResult result) {
        if (result.category() != requestedCategory) {
            throw new IllegalArgumentException("Analysis category does not match request");
        }
        String generatedText = String.join(" ", result.summary(), result.recommendedFor(),
                result.recommendationReason(), String.join(" ", result.topics()));
        if (FORBIDDEN_EXPRESSIONS.stream().anyMatch(generatedText::contains)) {
            throw new IllegalArgumentException("Analysis contains an unsupported claim");
        }
    }
}
