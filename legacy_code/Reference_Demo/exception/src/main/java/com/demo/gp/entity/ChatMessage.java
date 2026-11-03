package com.demo.gp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// 데모용 — 기능 구현 생략, Entity만 선언
@Entity
@Table(name = "CHAT_MESSAGES")
public class ChatMessage {

    @Id
    @Column(name = "message_id")
    private Integer messageId;

    @Column(name = "chat_room_id")
    private Integer chatRoomId;

    @Column(name = "sender_id")
    private Integer senderId;

    private String content;

    @Column(name = "is_read")
    private boolean read;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Integer getMessageId()                    { return messageId; }
    public void setMessageId(Integer id)             { this.messageId = id; }

    public Integer getChatRoomId()                   { return chatRoomId; }
    public void setChatRoomId(Integer id)            { this.chatRoomId = id; }

    public Integer getSenderId()                     { return senderId; }
    public void setSenderId(Integer id)              { this.senderId = id; }

    public String getContent()                       { return content; }
    public void setContent(String content)           { this.content = content; }

    public boolean isRead()                          { return read; }
    public void setRead(boolean read)                { this.read = read; }

    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime t)        { this.createdAt = t; }
}
