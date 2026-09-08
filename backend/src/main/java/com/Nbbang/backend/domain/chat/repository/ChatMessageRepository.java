package com.Nbbang.backend.domain.chat.repository;

import com.Nbbang.backend.domain.chat.entity.ChatMessage;
import com.Nbbang.backend.domain.chat.entity.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 채팅방 메시지 내역 (최근 50개) — CHAT, IMAGE 등 화면에 표시되는 타입만 (JOIN/LEAVE/READ 이벤트는 제외)
    List<ChatMessage> findTop50ByRoomIdAndTypeInOrderBySentAtAsc(Long roomId, List<MessageType> types);

    // [CHAT-RQ-002] 마지막 메시지 취소 시 채팅방 미리보기 재계산용 — 취소되지 않은 가장 최근 메시지
    java.util.Optional<ChatMessage> findTopByRoomIdAndTypeInAndDeletedFalseOrderBySentAtDesc(Long roomId, List<MessageType> types);

    // 안읽은 메시지 일괄 읽음 처리
    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.roomId = :roomId AND m.senderEmail != :readerEmail AND m.isRead = false")
    void markAsRead(@Param("roomId") Long roomId, @Param("readerEmail") String readerEmail);
}
