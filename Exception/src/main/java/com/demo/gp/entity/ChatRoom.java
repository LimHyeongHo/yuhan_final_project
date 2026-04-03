package com.demo.gp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// 데모용 — 기능 구현 생략, Entity만 선언
@Entity
@Table(name = "CHAT_ROOMS")
public class ChatRoom {

    @Id
    @Column(name = "chat_id")
    private Integer chatId;

    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "buyer_id")
    private Integer buyerId;

    @Column(name = "seller_id")
    private Integer sellerId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_message")
    private String lastMessage;

    @Column(name = "last_sent_at")
    private LocalDateTime lastSentAt;

    @Column(name = "unread_count")
    private int unreadCount;

    public Integer getChatId()                       { return chatId; }
    public void setChatId(Integer id)                { this.chatId = id; }

    public Integer getProductId()                    { return productId; }
    public void setProductId(Integer id)             { this.productId = id; }

    public Integer getBuyerId()                      { return buyerId; }
    public void setBuyerId(Integer id)               { this.buyerId = id; }

    public Integer getSellerId()                     { return sellerId; }
    public void setSellerId(Integer id)              { this.sellerId = id; }

    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime t)        { this.createdAt = t; }

    public String getLastMessage()                   { return lastMessage; }
    public void setLastMessage(String msg)           { this.lastMessage = msg; }

    public LocalDateTime getLastSentAt()             { return lastSentAt; }
    public void setLastSentAt(LocalDateTime t)       { this.lastSentAt = t; }

    public int getUnreadCount()                      { return unreadCount; }
    public void setUnreadCount(int n)                { this.unreadCount = n; }
}
