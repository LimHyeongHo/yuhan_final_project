package com.Nbbang.backend.domain.report.repository;

import com.Nbbang.backend.domain.report.entity.ChatReport;
import org.springframework.data.jpa.repository.JpaRepository;

// 채팅 신고
public interface ChatReportRepository extends JpaRepository<ChatReport, Long> {

    boolean existsByReporter_EmailAndMessage_Id(String reporterEmail, Long messageId);

    java.util.List<ChatReport> findAllByOrderByCreatedAtDesc();

    // 채팅 신고 — 신고된 구매자 목록의 미처리(PENDING) 개수
    long countByReported_EmailAndStatus(String reportedEmail, String status);
}
