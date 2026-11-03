package com.Nbbang.backend.domain.bookreport.service;

import com.Nbbang.backend.domain.bookreport.analysis.BookReportAnalysisRequest;
import com.Nbbang.backend.domain.bookreport.analysis.BookReportAnalysisResult;
import com.Nbbang.backend.domain.bookreport.analysis.BookReportAnalyzer;
import com.Nbbang.backend.domain.bookreport.config.DepartmentBookKeywordCatalog;
import com.Nbbang.backend.domain.bookreport.entity.BookReportStatus;
import com.Nbbang.backend.domain.bookreport.entity.DailyDepartmentBookReport;
import com.Nbbang.backend.domain.bookreport.entity.DepartmentBookAnalysis;
import com.Nbbang.backend.domain.bookreport.entity.KakaoBookCandidate;
import com.Nbbang.backend.domain.bookreport.repository.DailyDepartmentBookReportRepository;
import com.Nbbang.backend.domain.bookreport.repository.DepartmentBookAnalysisRepository;
import com.Nbbang.backend.domain.bookreport.repository.KakaoBookCandidateRepository;
import com.Nbbang.backend.domain.product.entity.ProductCategory;
import com.Nbbang.backend.domain.search.dto.KakaoBookSnapshot;
import com.Nbbang.backend.domain.search.service.KakaoBookSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DepartmentBookReportCollectionService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final String NOTICE_VERSION = "kakao-source-limit-v1";

    private final KakaoBookSearchService kakaoBookSearchService;
    private final DepartmentBookKeywordCatalog keywordCatalog;
    private final KakaoBookCandidateRepository candidateRepository;
    private final DepartmentBookAnalysisRepository analysisRepository;
    private final DailyDepartmentBookReportRepository reportRepository;
    private final BookReportAnalyzer analyzer;
    private final BookReportOutputGuard outputGuard;
    private final BookAttentionScoreCalculator scoreCalculator;
    private final BookReportJsonCodec jsonCodec;
    private final PlatformTransactionManager transactionManager;

    @Value("${book-report.analysis.min-confidence:0.70}")
    private double minimumConfidence;

    @Value("${book-report.recent-days:30}")
    private int recentDays;

    public DailyDepartmentBookReport collect(ProductCategory category) {
        DepartmentBookKeywordCatalog.DepartmentDefinition definition = keywordCatalog.get(category);
        LocalDateTime fetchedAt = LocalDateTime.now(Clock.system(SEOUL));
        LocalDate reportDate = fetchedAt.toLocalDate();
        Map<String, CollectedBook> collectedBooks = collectFromKakao(definition.keywords());
        List<DepartmentBookAnalysis> analyses = new ArrayList<>();
        boolean partial = false;
        int successfulAnalysisCount = 0;
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        for (CollectedBook collectedBook : collectedBooks.values()) {
            AnalysisOutcome outcome = Objects.requireNonNull(transactionTemplate.execute(status ->
                    saveAndAnalyze(category, collectedBook, fetchedAt, reportDate)));
            if (!outcome.succeeded()) {
                partial = true;
                continue;
            }
            successfulAnalysisCount++;
            if (outcome.analysis() != null) {
                analyses.add(outcome.analysis());
            }
        }

        if (!collectedBooks.isEmpty() && successfulAnalysisCount == 0) {
            throw new IllegalStateException("All book report analyses failed");
        }

        List<Long> itemIds = selectReportItems(analyses, reportDate).stream()
                .map(DepartmentBookAnalysis::getId)
                .toList();
        boolean finalPartial = partial;
        return Objects.requireNonNull(transactionTemplate.execute(status -> saveReport(
                category, fetchedAt, reportDate, itemIds, finalPartial)));
    }

    private DailyDepartmentBookReport saveReport(ProductCategory category, LocalDateTime fetchedAt,
                                                  LocalDate reportDate, List<Long> itemIds,
                                                  boolean partial) {
        DailyDepartmentBookReport report = reportRepository.findByReportDateAndCategory(reportDate, category)
                .orElseGet(DailyDepartmentBookReport::new);
        report.setReportDate(reportDate);
        report.setCategory(category);
        report.setStatus(partial ? BookReportStatus.PARTIAL : BookReportStatus.READY);
        report.setItemIdsJson(jsonCodec.writeLongs(itemIds));
        report.setGeneratedAt(LocalDateTime.now(Clock.system(SEOUL)));
        report.setSourceFetchedAt(fetchedAt);
        report.setKeywordConfigVersion(keywordCatalog.version());
        report.setNoticeVersion(NOTICE_VERSION);
        return reportRepository.save(report);
    }

    private Map<String, CollectedBook> collectFromKakao(List<String> keywords) {
        Map<String, CollectedBook> collected = new LinkedHashMap<>();
        for (String keyword : keywords) {
            for (KakaoBookSnapshot snapshot : kakaoBookSearchService.searchLatestBooks(keyword, 1, 50)) {
                if (snapshot.title() == null || snapshot.title().isBlank()) {
                    continue;
                }
                String deduplicationKey = BookCandidateKey.of(snapshot);
                CollectedBook book = collected.computeIfAbsent(
                        deduplicationKey, ignored -> new CollectedBook(snapshot));
                book.matchedKeywords().add(keyword);
            }
        }
        return collected;
    }

    private AnalysisOutcome saveAndAnalyze(ProductCategory category, CollectedBook collectedBook,
                                           LocalDateTime fetchedAt, LocalDate reportDate) {
        KakaoBookSnapshot snapshot = collectedBook.snapshot();
        String authorsJson = jsonCodec.writeStrings(snapshot.authors());
        KakaoBookCandidate candidate = findCandidate(snapshot, authorsJson)
                .orElseGet(KakaoBookCandidate::new);
        if (candidate.getId() == null) {
            candidate.setFirstSeenAt(fetchedAt);
        }
        candidate.setIsbn13(blankToNull(snapshot.isbn13()));
        candidate.setIsbn10(blankToNull(snapshot.isbn10()));
        candidate.setTitle(snapshot.title());
        candidate.setAuthorsJson(authorsJson);
        candidate.setPublisher(blankToNull(snapshot.publisher()));
        candidate.setContents(blankToNull(snapshot.contents()));
        candidate.setPublishedAt(snapshot.publishedAt());
        candidate.setPrice(snapshot.price());
        candidate.setSalePrice(snapshot.salePrice());
        candidate.setSaleStatusRaw(blankToNull(snapshot.saleStatus()));
        candidate.setSourceUrl(allowedSourceUrl(snapshot.sourceUrl()));
        candidate.setLastSeenAt(fetchedAt);
        candidate.setSourceFetchedAt(fetchedAt);
        candidate.setSourceHash(sourceHash(snapshot));
        candidate = candidateRepository.save(candidate);

        String promptVersion = analyzer.promptVersion();
        DepartmentBookAnalysis existing = analysisRepository
                .findByCandidateIdAndCategoryAndSourceHashAndPromptVersionAndKeywordConfigVersion(
                        candidate.getId(), category, candidate.getSourceHash(), promptVersion,
                        keywordCatalog.version())
                .orElse(null);
        if (existing != null) {
            return AnalysisOutcome.succeeded(existing);
        }

        BookReportAnalysisResult result;
        try {
            result = analyzer.analyze(new BookReportAnalysisRequest(
                    category, candidate.getTitle(), snapshot.authors(), candidate.getPublisher(),
                    candidate.getContents(), List.copyOf(collectedBook.matchedKeywords())));
            outputGuard.validate(category, result);
            if (!promptVersion.equals(result.promptVersion())) {
                throw new IllegalArgumentException("Analyzer prompt version does not match its result");
            }
        } catch (RuntimeException exception) {
            return AnalysisOutcome.failed();
        }
        if (!result.shouldInclude() || result.confidence() < minimumConfidence) {
            return AnalysisOutcome.succeeded(null);
        }

        DepartmentBookAnalysis analysis = new DepartmentBookAnalysis();
        analysis.setCandidate(candidate);
        analysis.setCategory(category);
        analysis.setKeywordConfigVersion(keywordCatalog.version());
        analysis.setMatchedKeywordsJson(jsonCodec.writeStrings(List.copyOf(collectedBook.matchedKeywords())));
        analysis.setRelevanceScore(result.relevanceScore());
        analysis.setAttentionScore(scoreCalculator.calculate(
                result.relevanceScore(), candidate.getPublishedAt(), candidate.getPrice(),
                candidate.getSalePrice(), informationComplete(candidate), reportDate));
        analysis.setDifficulty(result.difficulty());
        analysis.setTopicsJson(jsonCodec.writeStrings(result.topics()));
        analysis.setSummary(result.summary());
        analysis.setRecommendedFor(result.recommendedFor());
        analysis.setRecommendationReason(result.recommendationReason());
        analysis.setEvidenceFieldsJson(jsonCodec.writeStrings(result.evidenceFields()));
        analysis.setConfidence(result.confidence());
        analysis.setModelName(result.modelName());
        analysis.setPromptVersion(result.promptVersion());
        analysis.setAnalyzedAt(fetchedAt);
        analysis.setSourceHash(candidate.getSourceHash());
        return AnalysisOutcome.succeeded(analysisRepository.save(analysis));
    }

    private java.util.Optional<KakaoBookCandidate> findCandidate(KakaoBookSnapshot snapshot, String authorsJson) {
        if (snapshot.isbn13() != null && !snapshot.isbn13().isBlank()) {
            return candidateRepository.findByIsbn13(snapshot.isbn13());
        }
        if (snapshot.isbn10() != null && !snapshot.isbn10().isBlank()) {
            return candidateRepository.findFirstByIsbn10(snapshot.isbn10());
        }
        return candidateRepository.findFirstByTitleAndAuthorsJsonAndPublisher(
                snapshot.title(), authorsJson, blankToNull(snapshot.publisher()));
    }

    private List<DepartmentBookAnalysis> selectReportItems(List<DepartmentBookAnalysis> analyses,
                                                            LocalDate reportDate) {
        Comparator<DepartmentBookAnalysis> byAttention = Comparator
                .comparingInt(DepartmentBookAnalysis::getAttentionScore).reversed()
                .thenComparing(analysis -> analysis.getCandidate().getPublishedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(analysis -> analysis.getCandidate().getTitle());
        List<DepartmentBookAnalysis> newlyDiscovered = analyses.stream()
                .filter(analysis -> analysis.getCandidate().getFirstSeenAt().toLocalDate().equals(reportDate))
                .sorted(byAttention)
                .limit(5)
                .toList();
        List<Long> newIds = newlyDiscovered.stream().map(DepartmentBookAnalysis::getId).toList();
        List<DepartmentBookAnalysis> recentlyPublished = analyses.stream()
                .filter(analysis -> !newIds.contains(analysis.getId()))
                .filter(analysis -> analysis.getCandidate().getPublishedAt() != null)
                .filter(analysis -> !analysis.getCandidate().getPublishedAt().isBefore(reportDate.minusDays(recentDays)))
                .filter(analysis -> !analysis.getCandidate().getPublishedAt().isAfter(reportDate))
                .sorted(byAttention)
                .limit(5)
                .toList();
        List<DepartmentBookAnalysis> selected = new ArrayList<>(newlyDiscovered);
        selected.addAll(recentlyPublished);
        return selected;
    }

    private boolean informationComplete(KakaoBookCandidate candidate) {
        return candidate.getPublishedAt() != null
                && candidate.getPublisher() != null
                && candidate.getContents() != null
                && (candidate.getIsbn13() != null || candidate.getIsbn10() != null);
    }

    private String sourceHash(KakaoBookSnapshot snapshot) {
        String canonical = String.join("|",
                normalize(snapshot.title()), normalize(String.join(" ", snapshot.authors())),
                normalize(snapshot.publisher()), normalize(snapshot.contents()),
                String.valueOf(snapshot.publishedAt()), String.valueOf(snapshot.price()),
                String.valueOf(snapshot.salePrice()), normalize(snapshot.saleStatus()),
                normalize(snapshot.sourceUrl()));
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private String allowedSourceUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(value.trim());
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null
                    || !(isSameOrSubdomain(host, "daum.net") || isSameOrSubdomain(host, "kakao.com"))) {
                return null;
            }
            return uri.toString();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private boolean isSameOrSubdomain(String host, String allowedDomain) {
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        return normalizedHost.equals(allowedDomain) || normalizedHost.endsWith("." + allowedDomain);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record CollectedBook(KakaoBookSnapshot snapshot, LinkedHashSet<String> matchedKeywords) {
        private CollectedBook(KakaoBookSnapshot snapshot) {
            this(snapshot, new LinkedHashSet<>());
        }
    }

    private record AnalysisOutcome(DepartmentBookAnalysis analysis, boolean succeeded) {
        private static AnalysisOutcome succeeded(DepartmentBookAnalysis analysis) {
            return new AnalysisOutcome(analysis, true);
        }

        private static AnalysisOutcome failed() {
            return new AnalysisOutcome(null, false);
        }
    }
}
