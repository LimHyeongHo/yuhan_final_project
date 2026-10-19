package com.Nbbang.backend.domain.report.entity;

import com.Nbbang.backend.domain.auth.entity.UserAccount;
import com.Nbbang.backend.domain.chat.entity.ChatMessage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 채팅 신고 — 신고 1건 = 상대 메시지 1개 = 사유 1개. 같은 사람이 같은 메시지를 두 번 신고할 수 없음
@Entity
@Table(
        name = "chat_reports",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_reports_reporter_message",
                columnNames = {"reporter_email", "message_id"}))
@Getter
@Setter
public class ChatReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_report_id")
    private Long chatReportId;

    // 채팅 신고 — 메시지는 소프트 삭제라 보낸 사람이 지워도 원문을 message_id로 확인할 수 있어 원문은 따로 저장 안 함
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "message_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_reports_message"))
    private ChatMessage message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "reporter_email",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_reports_reporter"))
    private UserAccount reporter;

    // 채팅 신고 — 메시지 보낸 사람과 같은 값이지만, 관리자가 "이 사람이 받은 신고"를 바로 조회하려고 둠 (서버가 채움)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "reported_email",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_reports_reported"))
    private UserAccount reported;

    @Column(nullable = false, length = 50)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
