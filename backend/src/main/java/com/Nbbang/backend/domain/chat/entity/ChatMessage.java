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

    /// 상대가 읽었는지 (화면의 '1' 표시)
    @Column(nullable = false)
    private boolean isRead = false;

    /// 삭제해도 원문은 DB에 남기고 표시만 바꿈 (소프트 삭제)
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean deleted = false;

    @Column
    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();
}
