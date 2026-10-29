package com.Nbbang.backend.domain.chat.repository;

import com.Nbbang.backend.domain.chat.entity.ChatMessage;
import com.Nbbang.backend.domain.chat.entity.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 최신 메시지 50개를 최신순으로 가져오고
    // 화면에는 서비스에서 오래된 순으로 뒤집어 보여줌
    List<ChatMessage> findTop50ByRoomIdAndTypeInOrderBySentAtDesc(Long roomId, List<MessageType> types);

    // 삭제 안 된 가장 최근 메시지를 조회
    java.util.Optional<ChatMessage> findTopByRoomIdAndTypeInAndDeletedFalseOrderBySentAtDesc(Long roomId, List<MessageType> types);

    // 방에 들어오면 상대가 보낸 안 읽은 메시지를 한 번에 읽음 처리
    // (본인이 보낸 메시지는 제외됨)
    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.roomId = :roomId AND m.senderEmail != :readerEmail AND m.isRead = false")
    void markAsRead(@Param("roomId") Long roomId, @Param("readerEmail") String readerEmail);
}
