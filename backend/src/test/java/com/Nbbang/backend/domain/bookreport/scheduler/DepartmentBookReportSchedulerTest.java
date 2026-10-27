package com.Nbbang.backend.domain.bookreport.scheduler;

import com.Nbbang.backend.domain.bookreport.config.DepartmentBookKeywordCatalog;
import com.Nbbang.backend.domain.bookreport.service.DepartmentBookReportCollectionService;
import com.Nbbang.backend.domain.product.entity.ProductCategory;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashSet;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentBookReportSchedulerTest {

    @Mock
    private DepartmentBookKeywordCatalog keywordCatalog;
    @Mock
    private DepartmentBookReportCollectionService collectionService;

    private DepartmentBookReportScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new DepartmentBookReportScheduler(keywordCatalog, collectionService);
        ReflectionTestUtils.setField(scheduler, "enabled", true);
    }

    @Test
    void stopsTheBatchAfterKakaoQuotaIsExceeded() {
        ProductCategory first = ProductCategory.COMPUTER_SOFTWARE;
        ProductCategory second = ProductCategory.ARTIFICIAL_INTELLIGENCE;
        when(keywordCatalog.categories()).thenReturn(new LinkedHashSet<>(java.util.List.of(first, second)));
        when(collectionService.collect(first))
                .thenThrow(new CustomException(ErrorCode.BOOK_SEARCH_QUOTA_EXCEEDED));

        scheduler.collectDailyReports();

        verify(collectionService).collect(first);
        verify(collectionService, never()).collect(second);
    }

    @Test
    void continuesWithTheNextDepartmentAfterAnIndependentFailure() {
        ProductCategory first = ProductCategory.COMPUTER_SOFTWARE;
        ProductCategory second = ProductCategory.ARTIFICIAL_INTELLIGENCE;
        when(keywordCatalog.categories()).thenReturn(new LinkedHashSet<>(java.util.List.of(first, second)));
        when(collectionService.collect(first)).thenThrow(new IllegalStateException("failed"));

        scheduler.collectDailyReports();

        InOrder inOrder = inOrder(collectionService);
        inOrder.verify(collectionService).collect(first);
        inOrder.verify(collectionService).collect(second);
    }
}
