package com.Nbbang.backend.domain.bookreport.service;

import com.Nbbang.backend.domain.bookreport.entity.BookReportStatus;
import com.Nbbang.backend.domain.bookreport.entity.DailyDepartmentBookReport;
import com.Nbbang.backend.domain.product.entity.ProductCategory;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BookReportManualJobServiceTest {

    private final BookReportManualJobService jobService = new BookReportManualJobService();

    @Test
    void preventsAnotherManualJobWhileOneIsActive() {
        BookReportManualJobService.StartResult first =
                jobService.createJob(ProductCategory.COMPUTER_SOFTWARE);
        BookReportManualJobService.StartResult second =
                jobService.createJob(ProductCategory.MECHANICAL_ENGINEERING);

        assertThat(first.created()).isTrue();
        assertThat(second.created()).isFalse();
        assertThat(second.jobId()).isEqualTo(first.jobId());
        assertThat(second.snapshot()).containsEntry("status", "QUEUED");
    }

    @Test
    void recordsTheGeneratedReportAndReleasesTheManualJobLock() {
        BookReportManualJobService.StartResult first =
                jobService.createJob(ProductCategory.COMPUTER_SOFTWARE);
        DailyDepartmentBookReport report = new DailyDepartmentBookReport();
        report.setReportDate(LocalDate.of(2026, 11, 3));
        report.setStatus(BookReportStatus.READY);
        report.setGeneratedAt(LocalDateTime.of(2026, 11, 3, 7, 10));

        jobService.markRunning(first.jobId());
        jobService.complete(first.jobId(), report);

        Map<String, Object> completed = jobService.getJob(first.jobId());
        assertThat(completed)
                .containsEntry("status", "COMPLETED")
                .containsEntry("reportDate", "2026-11-03")
                .containsEntry("reportStatus", "READY")
                .containsEntry("generatedAt", "2026-11-03T07:10");
        assertThat(jobService.createJob(ProductCategory.MECHANICAL_ENGINEERING).created()).isTrue();
    }
}
