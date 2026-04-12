package com.springboot.chatting.chat;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Controller
public class ChatController {

    // 참여자 목록 추가
    public static final Set<String> participants = new HashSet<>();

    @MessageMapping("/chat.sendMessage")
    /// 메시지나 페이로드 수신할 때마다 /topic/public 큐에 추가
    @SendTo("/topic/public")
    public ChatMessage sendMessage(
            @Payload ChatMessage chatMessage
    ){
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(
            @Payload ChatMessage chatMessage,
            SimpMessageHeaderAccessor headerAccessor
    ){
        /// WebSocket Session에 사용자 이름 추가
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        participants.add(chatMessage.getSender());
        chatMessage.setParticipants(new ArrayList<>(participants)); // 참여자 목록 함께 전송
        return chatMessage;
    }
}
