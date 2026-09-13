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
    private final String targetEmail;     // 상대방 이메일 (구매자 화면의 판매자 프로필 카드 조회용)
    private final Long productId;         // [신규] 게시물별 채팅 목록 필터링용
    private final String productName;
    private final String lastMessage;
    private final LocalDateTime lastSentAt;
    private final int unreadCount;
    // [CHAT-RQ-001] 판매자가 이 방에서 나가 있는 상태인지 (방을 다시 열면 프론트가 재입장 API 호출)
    // 구매자가 나간 방은 애초에 목록에서 제외되므로 이 값은 항상 false.
    private final boolean iLeft;
    // [CHAT-RQ-001] 상대방(구매자↔판매자)이 탈퇴한 계정인지 — true면 프론트는 이름 대신 "탈퇴한 유저" 표시 + 입력창 비활성화
    private final boolean targetWithdrawn;
    // [CHAT-RQ-003] 이 방과 연결된 공동구매(상품)가 물리 삭제되었는지 — true면 상단 카드를 안내 배너로 대체 (채팅 자체는 계속 가능)
    private final boolean productDeleted;
    // [CHAT-RQ-003] 상품의 실제 최신 status(OPEN/CLOSED_SUCCESS/FULL 등) — 채팅방 헤더의 상태 텍스트가 이 값을 봐야
    // WebSocket 연결 여부와 무관하게 실제 거래 상태를 반영한다. 상품이 삭제됐거나 productId가 없으면 null.
    private final String productStatus;

    private ChatRoomResponse(Long roomId, String targetName, String targetEmail, Long productId, String productName,
                             String lastMessage, LocalDateTime lastSentAt, int unreadCount, boolean iLeft,
                             boolean targetWithdrawn, boolean productDeleted, String productStatus) {
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
    }

    public static ChatRoomResponse of(ChatRoom room, String myEmail, String targetNickname,
                                       boolean targetWithdrawn, boolean productDeleted, String productStatus) {
        boolean isBuyer = myEmail.equals(room.getBuyerEmail());
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
                productStatus
        );
    }
}
