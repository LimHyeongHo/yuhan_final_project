package com.Nbbang.backend.domain.report.controller;

import com.Nbbang.backend.domain.report.dto.ProductReportStatusUpdateRequest;
import com.Nbbang.backend.domain.report.service.ProductReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/product-reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class AdminProductReportController {

    private final ProductReportService productReportService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllReports() {
        return ResponseEntity.ok(productReportService.getAllReports());
    }

    @PatchMapping("/{reportId}/status")
    public ResponseEntity<Void> updateReportStatus(
            @PathVariable Long reportId,
            @RequestBody ProductReportStatusUpdateRequest request) {
        productReportService.updateReportStatus(reportId, request.status());
        return ResponseEntity.noContent().build();
    }
}
