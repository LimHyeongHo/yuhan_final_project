package com.Nbbang.backend.domain.report.repository;

import com.Nbbang.backend.domain.report.entity.ProductReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductReportRepository extends JpaRepository<ProductReport, Long> {

    boolean existsByReporter_EmailAndProduct_ProductId(String reporterEmail, Long productId);

    java.util.List<ProductReport> findAllByOrderByCreatedAtDesc();
}
