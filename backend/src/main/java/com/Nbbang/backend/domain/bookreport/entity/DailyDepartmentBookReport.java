package com.Nbbang.backend.domain.bookreport.entity;

import com.Nbbang.backend.domain.product.entity.ProductCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_department_book_report", uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_department_book_report", columnNames = {"report_date", "category"})
})
@Getter
@Setter
@NoArgsConstructor
public class DailyDepartmentBookReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private ProductCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookReportStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String itemIdsJson;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @Column(nullable = false)
    private LocalDateTime sourceFetchedAt;

    @Column(nullable = false, length = 40)
    private String keywordConfigVersion;

    @Column(nullable = false, length = 40)
    private String noticeVersion;
}
