package com.Nbbang.backend.domain.report.dto;

/// 신고 요청 형식 (신고당한 사람은 받지 않고 서버가 정함)
public record ChatReportCreateRequest(Long messageId, String reason, String detail) {
}
