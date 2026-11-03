package com.Nbbang.backend.domain.bookreport.controller;

import com.Nbbang.backend.domain.bookreport.config.DepartmentBookKeywordCatalog;
import com.Nbbang.backend.domain.bookreport.service.BookReportManualJobService;
import com.Nbbang.backend.domain.bookreport.service.BookReportManualWorker;
import com.Nbbang.backend.domain.product.entity.ProductCategory;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/book-reports")
@RequiredArgsConstructor
public class AdminBookReportController {

    private final DepartmentBookKeywordCatalog keywordCatalog;
    private final BookReportManualJobService jobService;
    private final BookReportManualWorker worker;

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generate(
            @RequestBody(required = false) ManualCollectionRequest request) {
        ProductCategory category = parseCategory(request == null ? null : request.category());
        BookReportManualJobService.StartResult startResult = jobService.createJob(category);
        if (!startResult.created()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(startResult.snapshot());
        }

        worker.run(startResult.jobId(), category);
        return ResponseEntity.accepted().body(startResult.snapshot());
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<Map<String, Object>> getJob(@PathVariable String jobId) {
        Map<String, Object> job = jobService.getJob(jobId);
        return job == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(job);
    }

    private ProductCategory parseCategory(String value) {
        if (value == null || value.isBlank()) {
            throw new CustomException(ErrorCode.BOOK_REPORT_INVALID_CATEGORY);
        }
        try {
            ProductCategory category = ProductCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
            keywordCatalog.get(category);
            return category;
        } catch (IllegalArgumentException exception) {
            throw new CustomException(ErrorCode.BOOK_REPORT_INVALID_CATEGORY);
        }
    }

    public record ManualCollectionRequest(String category) {
    }
}
