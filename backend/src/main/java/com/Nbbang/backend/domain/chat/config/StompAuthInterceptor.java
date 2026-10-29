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

// 웹소켓 연결, 구독 보안 검사
// 연결할 때 로그인 확인, 구독할 때 내 채팅방인지 확인
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
        // 로그인 안 했으면 웹소켓 연결을 거부함
        if (userId == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        String role = (String) sessionAttrs.getOrDefault("role", "ROLE_USER");
        // 세션의 이메일과 권한으로 웹소켓 사용자를 등록해 누가 보낸 메시지인지 구분
        accessor.setUser(new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority(role))));
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();

        // 전송 실패 알림은 본인에게만 오도록 /user/queue/errors 로만 구독 가능
        if (destination != null && destination.startsWith("/queue/")) {
            throw new CustomException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        // 본인 계정으로 온 채팅 알림은 본인만 구독 가능 (/topic/chat/user/{이메일})
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
        if (roomId == null) return;

        Principal principal = accessor.getUser();
        if (principal == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        String myEmail = principal.getName();
        ChatRoom room = chatRoomService.getRoom(roomId);
        // 채팅방 구매자, 판매자만 구독 가능 (chat_room 테이블 확인)
        if (!myEmail.equals(room.getBuyerEmail()) && !myEmail.equals(room.getSellerEmail())) {
            throw new CustomException(ErrorCode.CHAT_ACCESS_DENIED);
        }
    }

    private String extractUserTopicEmail(String destination) {
        String prefix = "/topic/chat/user/";
        if (destination == null || !destination.startsWith(prefix)) return null;
        String email = destination.substring(prefix.length());
        return email.isBlank() ? null : email;
    }

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
