package com.Nbbang.backend.domain.bookreport.analysis;

public interface BookReportAnalyzer {
    BookReportAnalysisResult analyze(BookReportAnalysisRequest request);

    String promptVersion();
}
