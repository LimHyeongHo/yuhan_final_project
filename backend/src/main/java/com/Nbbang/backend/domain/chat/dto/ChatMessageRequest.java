package com.Nbbang.backend.domain.chat.dto;

import com.Nbbang.backend.domain.chat.entity.MessageType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 메시지 전송 요청 형식 (사진은 업로드한 URL을 content에 담는다)
@Getter
@Setter
@NoArgsConstructor
public class ChatMessageRequest {
    private Long roomId;
    private String content;
    private MessageType type;
}
