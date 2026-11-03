package com.Nbbang.backend.domain.bookreport.service;

import com.Nbbang.backend.domain.bookreport.entity.DailyDepartmentBookReport;
import com.Nbbang.backend.domain.product.entity.ProductCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookReportManualWorker {

    private final DepartmentBookReportCollectionService collectionService;
    private final BookReportManualJobService jobService;

    @Async
    public void run(String jobId, ProductCategory category) {
        jobService.markRunning(jobId);
        try {
            DailyDepartmentBookReport report = collectionService.collect(category);
            jobService.complete(jobId, report);
        } catch (Exception exception) {
            log.warn("Manual book report collection failed: jobId={}, category={}, type={}",
                    jobId, category, exception.getClass().getSimpleName());
            jobService.fail(jobId, exception);
        }
    }
}
