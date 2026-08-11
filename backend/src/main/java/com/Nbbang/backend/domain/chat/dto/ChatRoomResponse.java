package com.Nbbang.backend.domain.chat.dto;

import com.Nbbang.backend.domain.chat.entity.ChatRoom;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 채팅방 목록 조회 시 프론트로 전달되는 응답 형식
 * 프론트 SharedChatPage 좌측 채팅방 목록에 사용
 */
@Getter
public class ChatRoomResponse {

    private final Long roomId;
    private final String targetName;      // 상대방 닉네임
    private final Long productId;         // [신규] 게시물별 채팅 목록 필터링용
    private final String productName;
    private final String lastMessage;
    private final LocalDateTime lastSentAt;
    private final int unreadCount;

    private ChatRoomResponse(Long roomId, String targetName, Long productId, String productName,
                             String lastMessage, LocalDateTime lastSentAt, int unreadCount) {
        this.roomId = roomId;
        this.targetName = targetName;
        this.productId = productId;
        this.productName = productName;
        this.lastMessage = lastMessage;
        this.lastSentAt = lastSentAt;
        this.unreadCount = unreadCount;
    }

    public static ChatRoomResponse of(ChatRoom room, String myEmail, String targetNickname) {
        int unreadCount = myEmail.equals(room.getBuyerEmail())
                ? room.getBuyerUnreadCount()
                : room.getSellerUnreadCount();

        return new ChatRoomResponse(
                room.getId(),
                targetNickname,
                room.getProductId(),
                room.getProductName(),
                room.getLastMessage(),
                room.getLastSentAt(),
                unreadCount
        );
    }
}
