package com.Nbbang.backend.domain.chat.dto;

import com.Nbbang.backend.domain.chat.entity.ChatRoom;
import lombok.Getter;

import java.time.LocalDateTime;

/// 채팅방 목록에 보여줄 응답 형식
@Getter
public class ChatRoomResponse {

    private final Long roomId;
    private final String targetName;      /// 상대방 닉네임
    private final String targetEmail;     /// 상대방 이메일
    private final Long productId;         /// 상품별 채팅 목록 필터용
    private final String productName;
    private final String lastMessage;
    private final LocalDateTime lastSentAt;
    private final int unreadCount;
    /// 판매자가 나간 방인지 (다시 열면 자동 재입장)
    private final boolean iLeft;
    /// 상대가 탈퇴한 계정인지
    private final boolean targetWithdrawn;
    /// 연결된 상품이 삭제되었는지
    private final boolean productDeleted;
    /// 상품의 현재 공동구매 상태
    private final String productStatus;
    /// 목록에 보여줄 상품 썸네일
    private final String productImageUrl;

    private ChatRoomResponse(Long roomId, String targetName, String targetEmail, Long productId, String productName,
                             String lastMessage, LocalDateTime lastSentAt, int unreadCount, boolean iLeft,
                             boolean targetWithdrawn, boolean productDeleted, String productStatus,
                             String productImageUrl) {
        this.roomId = roomId;
        this.targetName = targetName;
        this.targetEmail = targetEmail;
        this.productId = productId;
        this.productName = productName;
        this.lastMessage = lastMessage;
        this.lastSentAt = lastSentAt;
        this.unreadCount = unreadCount;
        this.iLeft = iLeft;
        this.targetWithdrawn = targetWithdrawn;
        this.productDeleted = productDeleted;
        this.productStatus = productStatus;
        this.productImageUrl = productImageUrl;
    }

    public static ChatRoomResponse of(ChatRoom room, String myEmail, String targetNickname,
                                       boolean targetWithdrawn, boolean productDeleted, String productStatus,
                                       String productImageUrl) {
        boolean isBuyer = myEmail.equals(room.getBuyerEmail());
        /// 본인이 구매자인지 판매자인지에 따라 안 읽은 수를 골라서 보냄
        int unreadCount = isBuyer ? room.getBuyerUnreadCount() : room.getSellerUnreadCount();
        boolean iLeft = !isBuyer && room.getSellerLeftAt() != null;
        String targetEmail = isBuyer ? room.getSellerEmail() : room.getBuyerEmail();

        return new ChatRoomResponse(
                room.getId(),
                targetNickname,
                targetEmail,
                room.getProductId(),
                room.getProductName(),
                room.getLastMessage(),
                room.getLastSentAt(),
                unreadCount,
                iLeft,
                targetWithdrawn,
                productDeleted,
                productStatus,
                productImageUrl
        );
    }
}
