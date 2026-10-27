package com.Nbbang.backend.domain.report.controller;

import com.Nbbang.backend.domain.report.dto.ChatReportStatusUpdateRequest;
import com.Nbbang.backend.domain.report.service.ChatReportService;
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

// 채팅 신고 — 관리자 전용 (/api/admin/** 는 WebMvcConfig에서 ROLE_ADMIN 확인)
@RestController
@RequestMapping("/api/admin/chat-reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class AdminChatReportController {

    private final ChatReportService chatReportService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllReports() {
        return ResponseEntity.ok(chatReportService.getAllReports());
    }

    @PatchMapping("/{reportId}/status")
    public ResponseEntity<Void> updateReportStatus(
            @PathVariable Long reportId,
            @RequestBody ChatReportStatusUpdateRequest request) {
        chatReportService.updateReportStatus(reportId, request.status());
        return ResponseEntity.noContent().build();
    }

    // 채팅 신고 — 신고된 구매자 목록 (판매자는 기존 판매자 목록에 채팅 신고 횟수가 이미 나옴)
    @GetMapping("/reported-buyers")
    public ResponseEntity<List<Map<String, Object>>> getReportedBuyers() {
        return ResponseEntity.ok(chatReportService.getReportedBuyers());
    }
}
