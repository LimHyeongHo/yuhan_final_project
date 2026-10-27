package com.Nbbang.backend.domain.report.dto;

// 채팅 신고 — 관리자 처리 상태 (RECEIVED / REJECTED)
public record ChatReportStatusUpdateRequest(String status) {
}
