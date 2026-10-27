package com.Nbbang.backend.domain.bookreport.scheduler;

import com.Nbbang.backend.domain.bookreport.config.DepartmentBookKeywordCatalog;
import com.Nbbang.backend.domain.bookreport.service.DepartmentBookReportCollectionService;
import com.Nbbang.backend.domain.product.entity.ProductCategory;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DepartmentBookReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(DepartmentBookReportScheduler.class);

    private final DepartmentBookKeywordCatalog keywordCatalog;
    private final DepartmentBookReportCollectionService collectionService;

    @Value("${book-report.collection.enabled:false}")
    private boolean enabled;

    @Scheduled(
            cron = "${book-report.collection.cron:0 0 4 * * *}",
            zone = "${book-report.collection.zone:Asia/Seoul}"
    )
    public void collectDailyReports() {
        if (!enabled) {
            return;
        }
        int successCount = 0;
        int failureCount = 0;
        for (ProductCategory category : keywordCatalog.categories()) {
            try {
                collectionService.collect(category);
                successCount++;
            } catch (CustomException exception) {
                failureCount++;
                log.warn("Department book report collection failed: category={}, errorCode={}",
                        category, exception.getErrorCode());
                if (exception.getErrorCode() == ErrorCode.BOOK_SEARCH_QUOTA_EXCEEDED) {
                    break;
                }
            } catch (RuntimeException exception) {
                failureCount++;
                log.warn("Department book report collection failed: category={}, errorType={}",
                        category, exception.getClass().getSimpleName());
            }
        }
        log.info("Department book report collection completed: targetCount={}, successCount={}, failureCount={}",
                keywordCatalog.categories().size(), successCount, failureCount);
    }
}
