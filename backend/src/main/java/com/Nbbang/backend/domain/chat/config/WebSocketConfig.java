package com.Nbbang.backend.domain.chat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    private final StompAuthInterceptor stompAuthInterceptor;

    // 연결이 살아 있는지 주기적으로 확인하는 신호(heartbeat) 전용 스케줄러
    private final ThreadPoolTaskScheduler heartbeatScheduler = createHeartbeatScheduler();

    private static ThreadPoolTaskScheduler createHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("stomp-heartbeat-");
        scheduler.setDaemon(true);
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 웹소켓 연결 진입점 (/ws)
        registry.addEndpoint("/ws")
                // 기존 로컬 호스트에서 사설망 호스트 + 모든 포트 허용으로 수정
                .setAllowedOrigins(allowedOrigins.split(","))
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 메시지 전송은 /app, 구독은 /topic, /queue 주소 사용
        registry.setApplicationDestinationPrefixes("/app");
        // 10초마다 연결 확인 신호로 끊긴 연결 감지
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(heartbeatScheduler);
    }

    // 연결, 구독 요청마다 보안 검사 실행
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthInterceptor);
    }
}
