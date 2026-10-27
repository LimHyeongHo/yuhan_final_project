package com.Nbbang.backend.domain.bookreport.service;

import com.Nbbang.backend.domain.bookreport.analysis.BookReportAnalyzer;
import com.Nbbang.backend.domain.bookreport.config.DepartmentBookKeywordCatalog;
import com.Nbbang.backend.domain.bookreport.entity.KakaoBookCandidate;
import com.Nbbang.backend.domain.bookreport.repository.DailyDepartmentBookReportRepository;
import com.Nbbang.backend.domain.bookreport.repository.DepartmentBookAnalysisRepository;
import com.Nbbang.backend.domain.bookreport.repository.KakaoBookCandidateRepository;
import com.Nbbang.backend.domain.product.entity.ProductCategory;
import com.Nbbang.backend.domain.search.dto.KakaoBookSnapshot;
import com.Nbbang.backend.domain.search.service.KakaoBookSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentBookReportCollectionServiceTest {

    @Mock
    private KakaoBookSearchService kakaoBookSearchService;
    @Mock
    private DepartmentBookKeywordCatalog keywordCatalog;
    @Mock
    private KakaoBookCandidateRepository candidateRepository;
    @Mock
    private DepartmentBookAnalysisRepository analysisRepository;
    @Mock
    private DailyDepartmentBookReportRepository reportRepository;
    @Mock
    private BookReportAnalyzer analyzer;
    @Mock
    private BookReportOutputGuard outputGuard;
    @Mock
    private BookAttentionScoreCalculator scoreCalculator;
    @Mock
    private PlatformTransactionManager transactionManager;

    private DepartmentBookReportCollectionService service;

    @BeforeEach
    void setUp() {
        service = new DepartmentBookReportCollectionService(
                kakaoBookSearchService,
                keywordCatalog,
                candidateRepository,
                analysisRepository,
                reportRepository,
                analyzer,
                outputGuard,
                scoreCalculator,
                new BookReportJsonCodec(),
                transactionManager
        );
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @Test
    void keepsTheExistingReportWhenEveryAnalysisFails() {
        ProductCategory category = ProductCategory.COMPUTER_SOFTWARE;
        KakaoBookSnapshot snapshot = new KakaoBookSnapshot(
                "자바 프로그래밍",
                List.of("홍길동"),
                "유한출판",
                "9781234567897",
                "123456789X",
                "프로그래밍 입문서",
                "https://search.daum.net/search?w=bookpage&bookId=1",
                LocalDate.now(),
                BigDecimal.valueOf(30000),
                BigDecimal.valueOf(27000),
                "정상판매"
        );
        when(keywordCatalog.get(category)).thenReturn(
                new DepartmentBookKeywordCatalog.DepartmentDefinition("컴퓨터소프트웨어전공", List.of("프로그래밍")));
        when(kakaoBookSearchService.searchLatestBooks("프로그래밍", 1, 50)).thenReturn(List.of(snapshot));
        when(candidateRepository.findByIsbn13(snapshot.isbn13())).thenReturn(Optional.empty());
        when(candidateRepository.save(any(KakaoBookCandidate.class))).thenAnswer(invocation -> {
            KakaoBookCandidate candidate = invocation.getArgument(0);
            candidate.setId(1L);
            return candidate;
        });
        when(analyzer.promptVersion()).thenReturn("baseline-v1");
        when(analysisRepository
                .findByCandidateIdAndCategoryAndSourceHashAndPromptVersionAndKeywordConfigVersion(
                        any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(analyzer.analyze(any())).thenThrow(new IllegalStateException("analysis unavailable"));

        assertThatThrownBy(() -> service.collect(category))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("All book report analyses failed");

        verify(candidateRepository).save(any(KakaoBookCandidate.class));
        verify(reportRepository, never()).save(any());
    }
}
