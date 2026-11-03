package com.Nbbang.backend.domain.bookreport.analysis;

import com.Nbbang.backend.domain.bookreport.entity.BookDifficulty;
import com.Nbbang.backend.domain.product.entity.ProductCategory;

import java.util.List;
import java.util.Set;

public record BookReportAnalysisResult(
        ProductCategory category,
        int relevanceScore,
        BookDifficulty difficulty,
        List<String> topics,
        String summary,
        String recommendedFor,
        String recommendationReason,
        List<String> evidenceFields,
        double confidence,
        boolean shouldInclude,
        String modelName,
        String promptVersion
) {
    private static final Set<String> ALLOWED_EVIDENCE_FIELDS = Set.of(
            "title", "authors", "publisher", "contents", "matchedKeywords"
    );

    public BookReportAnalysisResult {
        if (category == null || difficulty == null || relevanceScore < 0 || relevanceScore > 100
                || confidence < 0 || confidence > 1 || topics == null || evidenceFields == null
                || summary == null || recommendedFor == null || recommendationReason == null
                || modelName == null || promptVersion == null) {
            throw new IllegalArgumentException("Invalid book report analysis result");
        }
        if (summary.isBlank() || summary.length() > 1000
                || recommendedFor.isBlank() || recommendedFor.length() > 500
                || recommendationReason.isBlank() || recommendationReason.length() > 1000
                || modelName.isBlank() || modelName.length() > 100
                || promptVersion.isBlank() || promptVersion.length() > 40
                || topics.size() > 10 || evidenceFields.isEmpty()
                || evidenceFields.stream().anyMatch(field -> !ALLOWED_EVIDENCE_FIELDS.contains(field))) {
            throw new IllegalArgumentException("Book report analysis result violates the output schema");
        }
        topics = List.copyOf(topics);
        evidenceFields = List.copyOf(evidenceFields);
    }
}
