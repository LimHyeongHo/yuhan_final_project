package com.Nbbang.backend.domain.chat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message")
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false)
    private String senderEmail;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;

    @Column(nullable = false)
    private boolean isRead = false;

    // [CHAT-RQ-002] 메시지 전송 취소 — 원문은 DB에 남기되 조회 응답에서 가리고 표식을 표시(소프트 삭제)
    // 기존 행이 있는 테이블에 NOT NULL 컬럼을 추가하므로 DB 기본값(false)을 명시해 ddl-auto=update ALTER가 실패하지 않게 한다
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean deleted = false;

    @Column
    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();
}
