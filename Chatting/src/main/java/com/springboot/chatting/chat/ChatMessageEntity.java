package com.springboot.chatting.chat;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CHAT_MESSAGE")
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;
    private String sender;

    @Enumerated(EnumType.STRING)
    private MessageType type;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    public Long getId()                        { return id; }

    public String getContent()                 { return content; }
    public void setContent(String content)     { this.content = content; }

    public String getSender()                  { return sender; }
    public void setSender(String sender)       { this.sender = sender; }

    public MessageType getType()               { return type; }
    public void setType(MessageType type)      { this.type = type; }

    public LocalDateTime getSentAt()           { return sentAt; }
    public void setSentAt(LocalDateTime t)     { this.sentAt = t; }
}
