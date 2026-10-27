package com.Nbbang.backend.domain.bookreport.entity;

import com.Nbbang.backend.domain.product.entity.ProductCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "department_book_analysis", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_department_book_analysis_version",
                columnNames = {"candidate_id", "category", "source_hash", "prompt_version", "keyword_config_version"}
        )
})
@Getter
@Setter
@NoArgsConstructor
public class DepartmentBookAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private KakaoBookCandidate candidate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private ProductCategory category;

    @Column(name = "keyword_config_version", nullable = false, length = 40)
    private String keywordConfigVersion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String matchedKeywordsJson;

    @Column(nullable = false)
    private int relevanceScore;

    @Column(nullable = false)
    private int attentionScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookDifficulty difficulty;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String topicsJson;

    @Column(nullable = false, length = 1000)
    private String summary;

    @Column(nullable = false, length = 500)
    private String recommendedFor;

    @Column(nullable = false, length = 1000)
    private String recommendationReason;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String evidenceFieldsJson;

    @Column(nullable = false)
    private double confidence;

    @Column(nullable = false, length = 100)
    private String modelName;

    @Column(nullable = false, length = 40)
    private String promptVersion;

    @Column(nullable = false)
    private LocalDateTime analyzedAt;

    @Column(name = "source_hash", nullable = false, length = 64)
    private String sourceHash;
}
