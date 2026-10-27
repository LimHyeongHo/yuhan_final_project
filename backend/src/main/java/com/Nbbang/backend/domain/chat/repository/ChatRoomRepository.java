package com.Nbbang.backend.domain.chat.repository;

import com.Nbbang.backend.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /// 본인이 구매자나 판매자로 참여한 채팅방 목록
    List<ChatRoom> findByBuyerEmailOrSellerEmailOrderByLastSentAtDesc(String buyerEmail, String sellerEmail);

    /// 같은 구매자, 판매자, 상품의 기존 채팅방을 조회
    Optional<ChatRoom> findByBuyerEmailAndSellerEmailAndProductId(String buyerEmail, String sellerEmail, Long productId);
}
