package com.Nbbang.backend.domain.bookreport.analysis;

import com.Nbbang.backend.domain.product.entity.ProductCategory;

import java.util.List;

public record BookReportAnalysisRequest(
        ProductCategory category,
        String title,
        List<String> authors,
        String publisher,
        String contents,
        List<String> matchedKeywords
) {
}
