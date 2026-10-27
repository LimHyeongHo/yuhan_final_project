package com.Nbbang.backend.domain.report.controller;

import com.Nbbang.backend.domain.report.dto.ChatReportCreateRequest;
import com.Nbbang.backend.domain.report.service.ChatReportService;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 채팅 신고 — CORS는 /api/chat/** 공통 설정(ChatCorsConfig)을 따름
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatReportController {

    private final ChatReportService chatReportService;

    @PostMapping("/rooms/{roomId}/reports")
    public ResponseEntity<Void> createReport(
            @PathVariable Long roomId,
            @RequestBody ChatReportCreateRequest request,
            HttpSession session) {
        // 채팅 신고 1단계 — 로그인 세션
        String reporterEmail = session == null ? null : (String) session.getAttribute("userId");
        if (reporterEmail == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        chatReportService.createReport(roomId, reporterEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
