package com.Nbbang.backend.domain.report.controller;

import com.Nbbang.backend.domain.report.dto.ProductReportCreateRequest;
import com.Nbbang.backend.domain.report.service.ProductReportService;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class ProductReportController {

    private final ProductReportService productReportService;

    @PostMapping("/{productId}/reports")
    public ResponseEntity<Void> createReport(
            @PathVariable Long productId,
            @RequestBody ProductReportCreateRequest request,
            HttpSession session) {
        String reporterEmail = session == null ? null : (String) session.getAttribute("userId");
        if (reporterEmail == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        productReportService.createReport(productId, reporterEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
