package com.Nbbang.backend.domain.chat.controller;

import com.Nbbang.backend.domain.chat.dto.ChatMessageRequest;
import com.Nbbang.backend.domain.chat.dto.ChatMessageResponse;
import com.Nbbang.backend.domain.chat.entity.ChatRoom;
import com.Nbbang.backend.domain.chat.entity.MessageType;
import com.Nbbang.backend.domain.chat.service.ChatMessageService;
import com.Nbbang.backend.domain.chat.service.ChatRoomService;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket STOMP 메시지 핸들러
 *
 * 프론트 사용 예시:
 * stompClient.publish({
 *   destination: '/app/chat.message',
 *   body: JSON.stringify({ roomId: 1, content: '안녕하세요' })
 * });
 *
 * 구독:
 * stompClient.subscribe('/topic/chat/1', callback);
 */
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;
    private final SimpMessageSendingOperations messagingTemplate;

    @MessageMapping("/chat.message")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        String senderEmail = principal.getName();

        // 0. 방 소유권(buyer/seller) 검증 — SUBSCRIBE와 동일한 방식
        ChatRoom room = chatRoomService.getRoom(request.getRoomId());
        if (!senderEmail.equals(room.getBuyerEmail()) && !senderEmail.equals(room.getSellerEmail())) {
            throw new CustomException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        // 1. 빈 메시지 차단
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            return;
        }

        // 2. 메시지 저장
        MessageType type = request.getType() != null ? request.getType() : MessageType.CHAT;
        ChatMessageResponse response = chatMessageService.save(
                request.getRoomId(), senderEmail, request.getContent(), type);

        // 3. 채팅방 마지막 메시지 업데이트 (이미지는 URL 대신 미리보기 문구)
        String lastMessagePreview = type == MessageType.IMAGE ? "[사진]" : request.getContent();
        chatRoomService.updateLastMessage(request.getRoomId(), senderEmail, lastMessagePreview);

        // 4. 해당 채팅방 구독자 전체에게 브로드캐스트
        messagingTemplate.convertAndSend("/topic/chat/" + request.getRoomId(), response);
    }
}
