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

/// 사용자가 채팅 메시지를 신고하는 API (POST /api/chat/rooms/{방번호}/reports)
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
        /// 로그인한 사용자만 신고 가능
        String reporterEmail = session == null ? null : (String) session.getAttribute("userId");
        if (reporterEmail == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        chatReportService.createReport(roomId, reporterEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
