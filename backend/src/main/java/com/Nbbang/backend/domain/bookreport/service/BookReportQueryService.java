package com.Nbbang.backend.domain.bookreport.service;

import com.Nbbang.backend.domain.bookreport.config.DepartmentBookKeywordCatalog;
import com.Nbbang.backend.domain.bookreport.dto.BookReportResponse;
import com.Nbbang.backend.domain.bookreport.entity.BookReportStatus;
import com.Nbbang.backend.domain.bookreport.entity.DailyDepartmentBookReport;
import com.Nbbang.backend.domain.bookreport.entity.DepartmentBookAnalysis;
import com.Nbbang.backend.domain.bookreport.entity.KakaoBookCandidate;
import com.Nbbang.backend.domain.bookreport.repository.DailyDepartmentBookReportRepository;
import com.Nbbang.backend.domain.bookreport.repository.DepartmentBookAnalysisRepository;
import com.Nbbang.backend.domain.product.entity.ProductCategory;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookReportQueryService {

    public static final String NOTICE = "이 리포트는 카카오 책 검색 결과와 학과 적합도 자동 분석을 기반으로 하며 "
            + "전체 신간, 판매량, 베스트셀러 순위 또는 실제 구매 수요를 의미하지 않습니다.";

    private final DepartmentBookKeywordCatalog keywordCatalog;
    private final DailyDepartmentBookReportRepository reportRepository;
    private final DepartmentBookAnalysisRepository analysisRepository;
    private final BookReportJsonCodec jsonCodec;
    private final BookAttentionScoreCalculator scoreCalculator;

    @Transactional(readOnly = true)
    public BookReportResponse getLatest(String categoryValue) {
        ProductCategory category = parseCategory(categoryValue);
        DailyDepartmentBookReport report = reportRepository
                .findFirstByCategoryAndStatusInOrderByReportDateDescGeneratedAtDesc(
                        category, EnumSet.of(BookReportStatus.READY, BookReportStatus.PARTIAL))
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_REPORT_NOT_FOUND));
        List<Long> itemIds = jsonCodec.readLongs(report.getItemIdsJson());
        Map<Long, DepartmentBookAnalysis> analysisById = new HashMap<>();
        if (!itemIds.isEmpty()) {
            for (DepartmentBookAnalysis analysis : analysisRepository.findAllByIdIn(itemIds)) {
                analysisById.put(analysis.getId(), analysis);
            }
        }

        List<BookReportResponse.Book> newlyDiscovered = new ArrayList<>();
        List<BookReportResponse.Book> recentlyPublished = new ArrayList<>();
        for (Long itemId : itemIds) {
            DepartmentBookAnalysis analysis = analysisById.get(itemId);
            if (analysis == null) {
                continue;
            }
            BookReportResponse.Book book = toBook(analysis);
            if (analysis.getCandidate().getFirstSeenAt().toLocalDate().equals(report.getReportDate())) {
                newlyDiscovered.add(book);
            } else {
                recentlyPublished.add(book);
            }
        }

        List<BookReportResponse.Section> sections = List.of(
                new BookReportResponse.Section(
                        "NEWLY_DISCOVERED", "오늘 새로 발견된 도서", List.copyOf(newlyDiscovered)),
                new BookReportResponse.Section(
                        "RECENTLY_PUBLISHED", "최근 출판 주목 도서", List.copyOf(recentlyPublished))
        );
        return new BookReportResponse(
                report.getReportDate(),
                category,
                keywordCatalog.get(category).name(),
                report.getStatus(),
                report.getGeneratedAt(),
                new BookReportResponse.Source(
                        "KAKAO_DAUM_BOOK_SEARCH",
                        report.getSourceFetchedAt(),
                        report.getKeywordConfigVersion()),
                sections,
                NOTICE
        );
    }

    private ProductCategory parseCategory(String value) {
        if (value == null || value.isBlank()) {
            throw new CustomException(ErrorCode.BOOK_REPORT_INVALID_CATEGORY);
        }
        try {
            ProductCategory category = ProductCategory.valueOf(value.trim());
            keywordCatalog.get(category);
            return category;
        } catch (IllegalArgumentException exception) {
            throw new CustomException(ErrorCode.BOOK_REPORT_INVALID_CATEGORY);
        }
    }

    private BookReportResponse.Book toBook(DepartmentBookAnalysis analysis) {
        KakaoBookCandidate candidate = analysis.getCandidate();
        return new BookReportResponse.Book(
                analysis.getId(),
                candidate.getTitle(),
                jsonCodec.readStrings(candidate.getAuthorsJson()),
                candidate.getPublisher(),
                candidate.getPublishedAt(),
                candidate.getIsbn13() != null ? candidate.getIsbn13() : candidate.getIsbn10(),
                candidate.getPrice(),
                candidate.getSalePrice(),
                scoreCalculator.discountRate(candidate.getPrice(), candidate.getSalePrice()),
                analysis.getRelevanceScore(),
                analysis.getAttentionScore(),
                analysis.getDifficulty(),
                jsonCodec.readStrings(analysis.getTopicsJson()),
                analysis.getSummary(),
                analysis.getRecommendedFor(),
                analysis.getRecommendationReason(),
                candidate.getSourceUrl(),
                candidate.getSourceFetchedAt()
        );
    }
}
