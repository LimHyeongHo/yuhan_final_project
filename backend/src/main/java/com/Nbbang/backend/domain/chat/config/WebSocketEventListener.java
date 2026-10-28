package com.Nbbang.backend.domain.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

// 웹소켓 연결이 끊기면 로그 기록 (서버 콘솔에 출력됨)
@Component
@Slf4j
public class WebSocketEventListener {

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        log.info("WebSocket 연결 끊김 — sessionId: {}", sessionId);
    }
}
