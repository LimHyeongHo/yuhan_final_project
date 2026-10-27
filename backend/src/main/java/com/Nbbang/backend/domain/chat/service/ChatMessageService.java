package com.Nbbang.backend.domain.chat.service;

import com.Nbbang.backend.domain.auth.repository.UserAccountRepository;
import com.Nbbang.backend.domain.chat.dto.ChatMessageResponse;
import com.Nbbang.backend.domain.chat.entity.ChatMessage;
import com.Nbbang.backend.domain.chat.entity.ChatRoom;
import com.Nbbang.backend.domain.chat.entity.MessageType;
import com.Nbbang.backend.domain.chat.repository.ChatMessageRepository;
import com.Nbbang.backend.domain.chat.repository.ChatRoomRepository;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserAccountRepository userAccountRepository;

    /// 메시지를 DB에 저장하고 보낸 사람 닉네임을 붙여 응답 (chat_message 테이블)
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

    /// 입장, 퇴장 안내 메시지를 저장
    /// 시스템 메시지라 안 읽은 메시지로 세지 않음 (읽음 상태로 저장됨)
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

    /// 채팅방 최근 메시지 50개를 조회
    /// 구매자가 나갔다 다시 문의한 방이면 이전 메시지는 빼고 보여줌 (buyerHistoryFrom 기준)
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getHistory(ChatRoom room, String viewerEmail) {
        try {
            LocalDateTime clip = viewerEmail.equals(room.getBuyerEmail()) ? room.getBuyerHistoryFrom() : null;
            List<ChatMessageResponse> history = chatMessageRepository
                    /// 입장, 퇴장 안내도 내역에 포함
                    .findTop50ByRoomIdAndTypeInOrderBySentAtDesc(room.getId(),
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
            /// 화면은 오래된 순이라 다시 뒤집음
            Collections.reverse(history);
            return history;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.CHAT_HISTORY_LOAD_FAILED);
        }
    }

    @Transactional
    public void markAsRead(Long roomId, String readerEmail) {
        chatMessageRepository.markAsRead(roomId, readerEmail);
    }

    /// 채팅방 목록 미리보기에 보일 수 있는 메시지 종류 (글, 사진, 입장, 퇴장)
    private static final List<MessageType> PREVIEW_EVENT_TYPES =
            List.of(MessageType.CHAT, MessageType.IMAGE, MessageType.JOIN, MessageType.LEAVE);

    /// 메시지 삭제 (본인 메시지만, 실제로 지우지 않고 삭제 표시)
    /// 원문은 DB에 남아 있어서 신고가 들어오면 관리자가 확인 가능
    /// 삭제한 게 가장 최근 메시지면 목록 미리보기도 바뀜
    @Transactional
    public DeleteMessageResult deleteMessage(Long roomId, Long messageId, String requesterEmail) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));
        if (!message.getRoomId().equals(roomId)) {
            throw new CustomException(ErrorCode.CHAT_MESSAGE_NOT_FOUND);
        }
        if (!requesterEmail.equals(message.getSenderEmail())) {
            throw new CustomException(ErrorCode.CHAT_MESSAGE_NOT_OWNER);
        }

        /// 나간 사용자는 자기 메시지도 삭제 불가
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        boolean requesterLeft = requesterEmail.equals(room.getBuyerEmail())
                ? room.getBuyerLeftAt() != null
                : room.getSellerLeftAt() != null;
        if (requesterLeft) {
            throw new CustomException(ErrorCode.CHAT_ROOM_LEFT);
        }

        /// 지우는 메시지가 지금 미리보기에 보이는 메시지인지 확인
        Long currentLatestEventId = chatMessageRepository
                .findTopByRoomIdAndTypeInAndDeletedFalseOrderBySentAtDesc(roomId, PREVIEW_EVENT_TYPES)
                .map(ChatMessage::getId)
                .orElse(null);
        boolean wasLatestEvent = message.getId().equals(currentLatestEventId);

        /// 이미 삭제된 메시지면 그대로 둠 (여러 번 요청해도 결과 같음)
        if (!message.isDeleted()) {
            message.setDeleted(true);
            message.setDeletedAt(LocalDateTime.now());
        }

        if (!wasLatestEvent) {
            return new DeleteMessageResult(false, null);
        }

        String preview = chatMessageRepository
                .findTopByRoomIdAndTypeInAndDeletedFalseOrderBySentAtDesc(roomId, PREVIEW_EVENT_TYPES)
                .map(m -> m.getType() == MessageType.IMAGE ? "[사진]" : m.getContent())
                .orElse(null);
        return new DeleteMessageResult(true, preview);
    }

    /// 삭제 후 목록 미리보기를 바꿔야 하는지와 바꿀 문구를 함께 돌려줌
    public record DeleteMessageResult(boolean previewChanged, String newPreview) {}
}
