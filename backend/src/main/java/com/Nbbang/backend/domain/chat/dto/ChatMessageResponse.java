package com.Nbbang.backend.domain.chat.dto;

import com.Nbbang.backend.domain.chat.entity.ChatMessage;
import com.Nbbang.backend.domain.chat.entity.MessageType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.LocalDateTime;

// 웹소켓 전달과 내역 조회에 같이 쓰는 메시지 응답 형식
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
    private final boolean deleted;
    // 삭제 때문에 목록 미리보기 문구가 바뀌었는지 여부
    private final boolean previewChanged;

    public ChatMessageResponse(Long id, Long roomId, String senderEmail, String senderNickname,
                               String content, MessageType type, LocalDateTime sentAt, boolean isRead,
                               boolean deleted, boolean previewChanged) {
        this.id = id;
        this.roomId = roomId;
        this.senderEmail = senderEmail;
        this.senderNickname = senderNickname;
        this.content = content;
        this.type = type;
        this.sentAt = sentAt;
        this.isRead = isRead;
        this.deleted = deleted;
        this.previewChanged = previewChanged;
    }

    public static ChatMessageResponse from(ChatMessage message, String nickname) {
        boolean deleted = message.isDeleted();
        return new ChatMessageResponse(
                message.getId(),
                message.getRoomId(),
                message.getSenderEmail(),
                nickname,
                // 삭제된 메시지는 원문을 보내지 않음
                deleted ? "" : message.getContent(),
                message.getType(),
                message.getSentAt(),
                message.isRead(),
                deleted,
                false
        );
    }

    // 읽음 이벤트 (읽은 사람 이메일을 senderEmail 자리에 담음)
    public static ChatMessageResponse readEvent(Long roomId, String readerEmail) {
        return new ChatMessageResponse(
                null, roomId, readerEmail, null, null, MessageType.READ, LocalDateTime.now(), true, false, false);
    }

    // 메시지 삭제를 상대 화면에도 바로 알리는 이벤트
    public static ChatMessageResponse deleteEvent(Long roomId, Long messageId, boolean previewChanged, String lastMessagePreview) {
        return new ChatMessageResponse(
                messageId, roomId, null, null, lastMessagePreview, MessageType.DELETE, LocalDateTime.now(), true, true,
                previewChanged);
    }
}
