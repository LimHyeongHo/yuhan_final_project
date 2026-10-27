package com.Nbbang.backend.domain.bookreport.repository;

import com.Nbbang.backend.domain.bookreport.entity.BookReportStatus;
import com.Nbbang.backend.domain.bookreport.entity.DailyDepartmentBookReport;
import com.Nbbang.backend.domain.product.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

public interface DailyDepartmentBookReportRepository extends JpaRepository<DailyDepartmentBookReport, Long> {
    Optional<DailyDepartmentBookReport> findByReportDateAndCategory(LocalDate reportDate, ProductCategory category);

    Optional<DailyDepartmentBookReport> findFirstByCategoryAndStatusInOrderByReportDateDescGeneratedAtDesc(
            ProductCategory category, Collection<BookReportStatus> statuses);
}
