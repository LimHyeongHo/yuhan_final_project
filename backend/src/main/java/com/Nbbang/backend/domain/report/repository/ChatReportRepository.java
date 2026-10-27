package com.Nbbang.backend.domain.report.repository;

import com.Nbbang.backend.domain.report.entity.ChatReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatReportRepository extends JpaRepository<ChatReport, Long> {

    /// 이미 신고한 메시지인지 확인
    boolean existsByReporter_EmailAndMessage_Id(String reporterEmail, Long messageId);

    java.util.List<ChatReport> findAllByOrderByCreatedAtDesc();

    /// 신고당한 사람의 아직 처리 안 된 신고 개수
    long countByReported_EmailAndStatus(String reportedEmail, String status);
}
