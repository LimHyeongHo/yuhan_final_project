package com.springboot.chatting.chat;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatMessageService {

    private final ChatMessageRepository repo;

    public ChatMessageService(ChatMessageRepository repo) {
        this.repo = repo;
    }

    public void save(ChatMessage msg) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setContent(msg.getContent());
        entity.setSender(msg.getSender());
        entity.setType(msg.getType());
        entity.setSentAt(LocalDateTime.now());
        repo.save(entity);
    }

    // 최근 50개 CHAT 메시지를 ChatMessage DTO로 변환해서 반환
    public List<ChatMessage> getRecentMessages() {
        return repo.findTop50ByTypeOrderBySentAtAsc(MessageType.CHAT)
                .stream()
                .map(e -> ChatMessage.builder()
                        .sender(e.getSender())
                        .content(e.getContent())
                        .type(e.getType())
                        .build())
                .collect(Collectors.toList());
    }
}
