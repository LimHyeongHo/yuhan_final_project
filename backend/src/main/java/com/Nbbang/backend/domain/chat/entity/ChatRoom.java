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

    @Column(nullable = false)
    private int buyerUnreadCount = 0;

    @Column(nullable = false)
    private int sellerUnreadCount = 0;

    // [CHAT-RQ-001] 채팅방 나가기
    // null이면 참여 중, 값이 있으면 해당 사용자 목록에서 숨김.
    //  - 구매자: 나가면 영구(목록에서 빠지고, 다시 문의해도 이전 메시지는 복구되지 않음)
    //  - 판매자: 나가도 방을 다시 열면 재입장되고 이력이 그대로 복구됨
    @Column
    private LocalDateTime buyerLeftAt;

    @Column
    private LocalDateTime sellerLeftAt;

    // 구매자가 이 시각 이전 메시지는 보지 못함 (나가기 후 재문의 시 "복구 안 됨"을 위한 기준선). 판매자에겐 적용 안 됨.
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
