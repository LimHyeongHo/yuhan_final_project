package com.Nbbang.backend.domain.chat.service;

import com.Nbbang.backend.domain.auth.repository.UserAccountRepository;
import com.Nbbang.backend.domain.chat.dto.ChatMessageResponse;
import com.Nbbang.backend.domain.chat.entity.ChatMessage;
import com.Nbbang.backend.domain.chat.entity.ChatRoom;
import com.Nbbang.backend.domain.chat.entity.MessageType;
import com.Nbbang.backend.domain.chat.repository.ChatMessageRepository;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserAccountRepository userAccountRepository;

    /** 메시지 저장 후 응답 DTO 반환 */
    @Transactional
    public ChatMessageResponse save(Long roomId, String senderEmail, String content, MessageType type) {
        ChatMessage message = new ChatMessage();
        message.setRoomId(roomId);
        message.setSenderEmail(senderEmail);
        message.setContent(content);
        message.setType(type);
        chatMessageRepository.save(message);

        String nickname = userAccountRepository.findById(senderEmail)
                .map(u -> u.getNickname())
                .orElse(senderEmail);

        return ChatMessageResponse.from(message, nickname);
    }

    /**
     * [CHAT-RQ-001] 나가기/재입장 시스템 메시지 저장
     * 안읽음 계산에 잡히지 않도록 isRead=true로 저장하고, 내용은 서버에서 닉네임으로 조립한다.
     */
    @Transactional
    public ChatMessageResponse saveSystemMessage(Long roomId, String actorEmail, MessageType type) {
        String nickname = userAccountRepository.findById(actorEmail)
                .map(u -> u.getNickname())
                .orElse(actorEmail);
        String content = type == MessageType.LEAVE
                ? nickname + "님이 나가셨습니다"
                : nickname + "님이 들어왔습니다";

        ChatMessage message = new ChatMessage();
        message.setRoomId(roomId);
        message.setSenderEmail(actorEmail);
        message.setContent(content);
        message.setType(type);
        message.setRead(true);
        chatMessageRepository.save(message);

        return ChatMessageResponse.from(message, nickname);
    }

    /**
     * 채팅방 메시지 내역 조회 (최근 50개)
     * [CHAT-RQ-001] 구매자가 나갔다가 다시 문의한 방이면 buyerHistoryFrom 이전 메시지는 돌려주지 않는다(복구 불가).
     */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getHistory(ChatRoom room, String viewerEmail) {
        try {
            LocalDateTime clip = viewerEmail.equals(room.getBuyerEmail()) ? room.getBuyerHistoryFrom() : null;
            return chatMessageRepository
                    // JOIN/LEAVE 시스템 메시지도 내역에 포함해 가운데 안내로 표시
                    .findTop50ByRoomIdAndTypeInOrderBySentAtAsc(room.getId(),
                            List.of(MessageType.CHAT, MessageType.IMAGE, MessageType.JOIN, MessageType.LEAVE))
                    .stream()
                    .filter(msg -> clip == null || !msg.getSentAt().isBefore(clip))
                    .map(msg -> {
                        String nickname = userAccountRepository.findById(msg.getSenderEmail())
                                .map(u -> u.getNickname())
                                .orElse(msg.getSenderEmail());
                        return ChatMessageResponse.from(msg, nickname);
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.CHAT_HISTORY_LOAD_FAILED);
        }
    }

    /** 안읽은 메시지 읽음 처리 */
    @Transactional
    public void markAsRead(Long roomId, String readerEmail) {
        chatMessageRepository.markAsRead(roomId, readerEmail);
    }

    /**
     * [CHAT-RQ-002] 메시지 전송 취소 (소프트 삭제)
     * 작성자 본인만 가능 → 타인 요청은 CHAT_MESSAGE_NOT_OWNER(403).
     * 이미 취소된 메시지를 다시 호출해도 결과가 같도록 멱등하게 처리한다.
     * @return 취소 후 채팅방 목록에 표시할 마지막 메시지 미리보기 (남은 메시지가 없으면 null)
     */
    @Transactional
    public String deleteMessage(Long roomId, Long messageId, String requesterEmail) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));
        if (!message.getRoomId().equals(roomId)) {
            throw new CustomException(ErrorCode.CHAT_MESSAGE_NOT_FOUND);
        }
        if (!requesterEmail.equals(message.getSenderEmail())) {
            throw new CustomException(ErrorCode.CHAT_MESSAGE_NOT_OWNER);
        }
        if (!message.isDeleted()) {
            message.setDeleted(true);
            message.setDeletedAt(LocalDateTime.now());
        }
        return chatMessageRepository
                .findTopByRoomIdAndTypeInAndDeletedFalseOrderBySentAtDesc(
                        roomId, List.of(MessageType.CHAT, MessageType.IMAGE))
                .map(m -> m.getType() == MessageType.IMAGE ? "[사진]" : m.getContent())
                .orElse(null);
    }
}
