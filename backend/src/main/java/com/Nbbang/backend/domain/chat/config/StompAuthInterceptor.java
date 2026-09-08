package com.Nbbang.backend.domain.chat.config;

import com.Nbbang.backend.domain.chat.entity.ChatRoom;
import com.Nbbang.backend.domain.chat.service.ChatRoomService;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * WebSocket CONNECT 시 HTTP 세션에서 userId를 읽어 Principal로 설정합니다.
 * PKI 로그인 성공 시 session.setAttribute("userId", email) 저장이 전제됩니다.
 *
 * [수정] CONNECT 시 userId 없으면 연결 자체를 거부하고,
 * SUBSCRIBE 시 해당 roomId의 buyer/seller인지 확인해 아무나 남의 채팅방을 구독하지 못하게 함.
 */
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final ChatRoomService chatRoomService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            handleSubscribe(accessor);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
        String userId = sessionAttrs != null ? (String) sessionAttrs.get("userId") : null;
        if (userId == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        String role = (String) sessionAttrs.getOrDefault("role", "ROLE_USER");
        accessor.setUser(new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority(role))));
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();

        // 전역 알림용 개인 토픽 "/topic/chat/user/{email}" — 본인 것만 구독 허용
        String userTopicEmail = extractUserTopicEmail(destination);
        if (userTopicEmail != null) {
            Principal principal = accessor.getUser();
            if (principal == null) {
                throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
            }
            if (!userTopicEmail.equals(principal.getName())) {
                throw new CustomException(ErrorCode.CHAT_ACCESS_DENIED);
            }
            return;
        }

        Long roomId = extractRoomId(destination);
        if (roomId == null) return; // 채팅방 토픽이 아니면 통과 (다른 브로드캐스트 용도 대비)

        Principal principal = accessor.getUser();
        if (principal == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        String myEmail = principal.getName();
        ChatRoom room = chatRoomService.getRoom(roomId);
        if (!myEmail.equals(room.getBuyerEmail()) && !myEmail.equals(room.getSellerEmail())) {
            throw new CustomException(ErrorCode.CHAT_ACCESS_DENIED);
        }
    }

    /** "/topic/chat/user/{email}" 형태에서 email 추출, 아니면 null */
    private String extractUserTopicEmail(String destination) {
        String prefix = "/topic/chat/user/";
        if (destination == null || !destination.startsWith(prefix)) return null;
        String email = destination.substring(prefix.length());
        return email.isBlank() ? null : email;
    }

    /** "/topic/chat/{roomId}" 형태에서 roomId 추출, 형식 안 맞으면 null */
    private Long extractRoomId(String destination) {
        if (destination == null || !destination.startsWith("/topic/chat/")) return null;
        String idPart = destination.substring("/topic/chat/".length());
        try {
            return Long.parseLong(idPart);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
