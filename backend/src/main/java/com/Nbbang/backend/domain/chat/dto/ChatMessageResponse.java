package com.Nbbang.backend.domain.chat.dto;

import com.Nbbang.backend.domain.chat.entity.ChatMessage;
import com.Nbbang.backend.domain.chat.entity.MessageType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 서버 → 프론트로 전달되는 메시지 응답 형식
 * WebSocket 브로드캐스트와 REST 메시지 내역 조회 모두에 사용
 */
@Getter
public class ChatMessageResponse {

    private final Long id;
    private final Long roomId;
    private final String senderEmail;
    private final String senderNickname;
    private final String content;
    private final MessageType type;
    private final LocalDateTime sentAt;
    @JsonProperty("isRead")
    private final boolean isRead;
    // [CHAT-RQ-002] 전송 취소된 메시지 여부 — true면 content는 비워서 보낸다
    private final boolean deleted;

    public ChatMessageResponse(Long id, Long roomId, String senderEmail, String senderNickname,
                               String content, MessageType type, LocalDateTime sentAt, boolean isRead,
                               boolean deleted) {
        this.id = id;
        this.roomId = roomId;
        this.senderEmail = senderEmail;
        this.senderNickname = senderNickname;
        this.content = content;
        this.type = type;
        this.sentAt = sentAt;
        this.isRead = isRead;
        this.deleted = deleted;
    }

    public static ChatMessageResponse from(ChatMessage message, String nickname) {
        boolean deleted = message.isDeleted();
        return new ChatMessageResponse(
                message.getId(),
                message.getRoomId(),
                message.getSenderEmail(),
                nickname,
                // [CHAT-RQ-002] 취소된 메시지는 원문을 내려주지 않는다 (프론트에서 표식으로 대체)
                deleted ? "" : message.getContent(),
                message.getType(),
                message.getSentAt(),
                message.isRead(),
                deleted
        );
    }

    /**
     * READ 이벤트 전용 — 누가 읽었는지(readerEmail)를 senderEmail 자리에 실어 보낸다.
     * 프론트는 "readerEmail != 내 이메일"일 때만 내 말풍선의 1을 지운다 (내가 읽음 처리한 이벤트는 무시).
     */
    public static ChatMessageResponse readEvent(Long roomId, String readerEmail) {
        return new ChatMessageResponse(
                null, roomId, readerEmail, null, null, MessageType.READ, LocalDateTime.now(), true, false);
    }

    /**
     * [CHAT-RQ-002] 메시지 전송 취소 이벤트 전용
     * content에는 채팅방 목록 미리보기 갱신용 마지막 메시지 문구를 담아 보낸다.
     */
    public static ChatMessageResponse deleteEvent(Long roomId, Long messageId, String lastMessagePreview) {
        return new ChatMessageResponse(
                messageId, roomId, null, null, lastMessagePreview, MessageType.DELETE, LocalDateTime.now(), true, true);
    }
}
