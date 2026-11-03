package com.springboot.chatting.config;

import com.springboot.chatting.chat.ChatController;
import com.springboot.chatting.chat.ChatMessage;
import com.springboot.chatting.chat.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j  /// 사용자가 채팅을 나갈 때 정보 기록
public class WebSocketEventListener {

    private final SimpMessageSendingOperations messagingTemplate;
    /// 세션 끊김 이벤트
    @EventListener
    public void handleWebSocketDisconnectListener(
            SessionDisconnectEvent event
    ){
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        if(username != null){
            ChatController.participants.remove(username);
            log.info("User Disconnected : {}" , username);
            var chatMessage = ChatMessage.builder()
                    .type(MessageType.LEAVE)
                    .sender(username)
                    .build();
            messagingTemplate.convertAndSend("/topic/public", chatMessage);
        }
    }
}
