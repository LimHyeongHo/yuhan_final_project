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
import com.Nbbang.backend.global.exception.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

// 웹소켓으로 들어온 채팅 메시지 처리
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;
    private final UserAccountRepository userAccountRepository;
    private final SimpMessageSendingOperations messagingTemplate;

    // 사용자는 일반 글, 사진 메시지만 보낼 수 있음
    private static final List<MessageType> ALLOWED_CLIENT_MESSAGE_TYPES = List.of(MessageType.CHAT, MessageType.IMAGE);

    // 메시지 전송 진입점 (/app/chat.message)
    // 검증 → DB 저장 → 방 구독자에게 전달 → 상대 개인 알림 순서로 처리됨
    @MessageMapping("/chat.message")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        String senderEmail = principal.getName();

        // 채팅방 구매자, 판매자만 메시지 전송 가능
        ChatRoom room = chatRoomService.getRoom(request.getRoomId());
        boolean senderIsBuyer = senderEmail.equals(room.getBuyerEmail());
        if (!senderIsBuyer && !senderEmail.equals(room.getSellerEmail())) {
            throw new CustomException(ErrorCode.CHAT_ACCESS_DENIED);
        }
        String recipient = senderIsBuyer ? room.getSellerEmail() : room.getBuyerEmail();

        // 나간 방, 탈퇴 유저는 서버에서 메시지 전송을 차단하기
        boolean senderLeft = senderIsBuyer ? room.getBuyerLeftAt() != null : room.getSellerLeftAt() != null;
        if (senderLeft) {
            throw new CustomException(ErrorCode.CHAT_ROOM_LEFT);
        }

        boolean recipientWithdrawn = userAccountRepository.findById(recipient)
                .map(u -> "WITHDRAWN".equals(u.getStatus()))
                .orElse(false);
        if (recipientWithdrawn) {
            throw new CustomException(ErrorCode.CHAT_RECIPIENT_WITHDRAWN);
        }

        // 입장, 퇴장 같은 시스템 메시지 위조 차단
        if (request.getType() != null && !ALLOWED_CLIENT_MESSAGE_TYPES.contains(request.getType())) {
            throw new CustomException(ErrorCode.CHAT_INVALID_MESSAGE_TYPE);
        }

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            return;
        }

        // 메시지 DB 저장 (chat_message 테이블)
        MessageType type = request.getType() != null ? request.getType() : MessageType.CHAT;
        ChatMessageResponse response = chatMessageService.save(
                request.getRoomId(), senderEmail, request.getContent(), type);

        // 채팅방 목록에 보일 마지막 메시지 갱신
        String lastMessagePreview = type == MessageType.IMAGE ? "[사진]" : request.getContent();
        chatRoomService.updateLastMessage(request.getRoomId(), senderEmail, lastMessagePreview);

        // 채팅방 구독자에게 실시간 전달 (/topic/chat/{방번호})
        messagingTemplate.convertAndSend("/topic/chat/" + request.getRoomId(), response);

        // 상대가 다른 페이지에 있어도 알림 전송 (방을 나갔으면 생략)
        boolean recipientLeft = senderIsBuyer
                ? room.getSellerLeftAt() != null
                : room.getBuyerLeftAt() != null;
        if (!recipientLeft) {
            messagingTemplate.convertAndSend("/topic/chat/user/" + recipient, response);
        }
    }

    // 전송 실패는 보낸 사람에게만 에러를 전달
    @MessageExceptionHandler(CustomException.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public ErrorResponse handleSendError(CustomException e) {
        return ErrorResponse.of(e.getErrorCode(), "/app/chat.message");
    }

    // 예상 못한 오류는 로그만 남기고 고정 문구로 응답
    @MessageExceptionHandler(Exception.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public ErrorResponse handleUnexpectedSendError(Exception e) {
        log.error("채팅 메시지 전송 처리 중 예외", e);
        return ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, "/app/chat.message");
    }
}
