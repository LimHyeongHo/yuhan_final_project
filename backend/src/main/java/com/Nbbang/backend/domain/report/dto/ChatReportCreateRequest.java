package com.Nbbang.backend.domain.report.dto;

// 채팅 신고 — 신고할 메시지 id + 사유 + 상세 내용. 신고당한 사람은 받지 않고 서버가 메시지 보낸 사람으로 채움
public record ChatReportCreateRequest(Long messageId, String reason, String detail) {
}
