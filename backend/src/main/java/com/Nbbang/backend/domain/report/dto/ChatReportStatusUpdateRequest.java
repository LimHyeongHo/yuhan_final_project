package com.Nbbang.backend.domain.report.dto;

/// 관리자가 정하는 처리 상태 (접수 또는 반려)
public record ChatReportStatusUpdateRequest(String status) {
}
