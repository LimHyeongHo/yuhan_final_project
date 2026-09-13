package com.Nbbang.backend.domain.chat.controller;

import com.Nbbang.backend.domain.auth.repository.UserAccountRepository;
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
import java.util.List;

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
    private final UserAccountRepository userAccountRepository;
    private final SimpMessageSendingOperations messagingTemplate;

    // [CHAT-RQ-002] 클라이언트가 직접 보낼 수 있는 메시지 타입 화이트리스트.
    // JOIN/LEAVE/READ/DELETE는 서버가 REST 엔드포인트(leave/rejoin/read/messages DELETE)에서만 내부적으로 생성한다.
    private static final List<MessageType> ALLOWED_CLIENT_MESSAGE_TYPES = List.of(MessageType.CHAT, MessageType.IMAGE);

    @MessageMapping("/chat.message")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        String senderEmail = principal.getName();

        // 0. 방 소유권(buyer/seller) 검증 — SUBSCRIBE와 동일한 방식
        ChatRoom room = chatRoomService.getRoom(request.getRoomId());
        boolean senderIsBuyer = senderEmail.equals(room.getBuyerEmail());
        if (!senderIsBuyer && !senderEmail.equals(room.getSellerEmail())) {
            throw new CustomException(ErrorCode.CHAT_ACCESS_DENIED);
        }
        String recipient = senderIsBuyer ? room.getSellerEmail() : room.getBuyerEmail();

        // 0-1. [CHAT-RQ-001] 발신자 본인이 이 방을 나간 상태면 전송 차단 (기존엔 수신자 leftAt만 확인했음)
        boolean senderLeft = senderIsBuyer ? room.getBuyerLeftAt() != null : room.getSellerLeftAt() != null;
        if (senderLeft) {
            throw new CustomException(ErrorCode.CHAT_ROOM_LEFT);
        }

        // 0-2. [CHAT-RQ-001] 수신자가 탈퇴한 계정이면 전송 차단 — 프론트 입력창 비활성화만으로는
        //      STOMP 클라이언트를 직접 다뤄 우회할 수 있으므로 서버에서도 동일하게 검증한다.
        boolean recipientWithdrawn = userAccountRepository.findById(recipient)
                .map(u -> "WITHDRAWN".equals(u.getStatus()))
                .orElse(false);
        if (recipientWithdrawn) {
            throw new CustomException(ErrorCode.CHAT_RECIPIENT_WITHDRAWN);
        }

        // 0-3. [CHAT-RQ-002] 시스템 전용 타입(JOIN/LEAVE 등) 위조 차단 — 화이트리스트 방식
        if (request.getType() != null && !ALLOWED_CLIENT_MESSAGE_TYPES.contains(request.getType())) {
            throw new CustomException(ErrorCode.CHAT_INVALID_MESSAGE_TYPE);
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

        // 5. 수신자 개인 토픽에도 발행 — 채팅 화면 밖(다른 페이지)에 있어도 헤더 알림이 실시간 갱신되도록
        //    단, 수신자가 이 방에서 나가 있으면 알림(토스트/헤더 배지)을 보내지 않는다.
        boolean recipientLeft = senderIsBuyer
                ? room.getSellerLeftAt() != null
                : room.getBuyerLeftAt() != null;
        if (!recipientLeft) {
            messagingTemplate.convertAndSend("/topic/chat/user/" + recipient, response);
        }
    }
}
