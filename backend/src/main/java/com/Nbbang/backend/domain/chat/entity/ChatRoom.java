package com.Nbbang.backend.domain.chat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_room")
@Getter
@Setter
@NoArgsConstructor
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String buyerEmail;

    @Column(nullable = false)
    private String sellerEmail;

    @Column
    private Long productId;

    @Column
    private String productName;

    @Column(length = 500)
    private String lastMessage;

    @Column
    private LocalDateTime lastSentAt;

    /// 구매자, 판매자 각각의 안 읽은 메시지 수
    @Column(nullable = false)
    private int buyerUnreadCount = 0;

    @Column(nullable = false)
    private int sellerUnreadCount = 0;

    /// 나간 시각 (구매자는 영구 나가기, 판매자는 다시 열면 재입장)
    @Column
    private LocalDateTime buyerLeftAt;

    @Column
    private LocalDateTime sellerLeftAt;

    /// 구매자가 나갔다 다시 문의하면 이 시각 이전 메시지는 보이지 않음
    @Column
    private LocalDateTime buyerHistoryFrom;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public static ChatRoom create(String buyerEmail, String sellerEmail, Long productId, String productName) {
        ChatRoom room = new ChatRoom();
        room.buyerEmail = buyerEmail;
        room.sellerEmail = sellerEmail;
        room.productId = productId;
        room.productName = productName;
        return room;
    }
}
