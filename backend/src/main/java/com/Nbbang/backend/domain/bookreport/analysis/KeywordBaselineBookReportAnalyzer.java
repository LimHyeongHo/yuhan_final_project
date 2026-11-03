package com.Nbbang.backend.domain.bookreport.analysis;

import com.Nbbang.backend.domain.bookreport.entity.BookDifficulty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
@ConditionalOnProperty(
        name = "book-report.analysis.provider",
        havingValue = "baseline",
        matchIfMissing = true
)
public class KeywordBaselineBookReportAnalyzer implements BookReportAnalyzer {

    public static final String MODEL_NAME = "KEYWORD_BASELINE";
    public static final String PROMPT_VERSION = "baseline-v1";

    @Override
    public BookReportAnalysisResult analyze(BookReportAnalysisRequest request) {
        String title = safe(request.title());
        String contents = safe(request.contents());
        String searchable = (title + " " + contents).toLowerCase(Locale.ROOT);
        long matchedCount = request.matchedKeywords().stream()
                .filter(keyword -> searchable.contains(keyword.toLowerCase(Locale.ROOT)))
                .count();
        int relevanceScore = Math.min(100, 55 + (int) matchedCount * 15 + (title.isBlank() ? 0 : 10));
        BookDifficulty difficulty = inferDifficulty(searchable);
        String summary = contents.isBlank() ? title : truncate(contents, 240);
        List<String> topics = request.matchedKeywords().stream().distinct().limit(3).toList();

        return new BookReportAnalysisResult(
                request.category(),
                relevanceScore,
                difficulty,
                topics,
                summary,
                "해당 학과 주제를 학습하려는 학생",
                "제목과 책 소개에서 학과 검색어가 확인되어 신규 도서 후보로 분류했습니다.",
                List.of("title", "contents", "matchedKeywords"),
                matchedCount > 0 ? 0.78 : 0.55,
                matchedCount > 0,
                MODEL_NAME,
                PROMPT_VERSION
        );
    }

    @Override
    public String promptVersion() {
        return PROMPT_VERSION;
    }

    private BookDifficulty inferDifficulty(String text) {
        if (text.contains("고급") || text.contains("심화") || text.contains("전문가")) {
            return BookDifficulty.ADVANCED;
        }
        if (text.contains("입문") || text.contains("기초") || text.contains("초보")) {
            return BookDifficulty.BEGINNER;
        }
        return text.isBlank() ? BookDifficulty.UNKNOWN : BookDifficulty.INTERMEDIATE;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
