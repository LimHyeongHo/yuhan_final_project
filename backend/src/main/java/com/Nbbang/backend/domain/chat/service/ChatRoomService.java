package com.Nbbang.backend.domain.chat.service;

import com.Nbbang.backend.domain.auth.repository.UserAccountRepository;
import com.Nbbang.backend.domain.chat.dto.ChatRoomResponse;
import com.Nbbang.backend.domain.chat.entity.ChatRoom;
import com.Nbbang.backend.domain.chat.repository.ChatRoomRepository;
import com.Nbbang.backend.domain.product.repository.ProductRepository;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserAccountRepository userAccountRepository;
    private final ProductRepository productRepository;

    /** 내 채팅방 목록 조회 */
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getRooms(String myEmail) {
        List<ChatRoom> rooms = chatRoomRepository
                .findByBuyerEmailOrSellerEmailOrderByLastSentAtDesc(myEmail, myEmail);

        return rooms.stream()
                .map(room -> {
                    String targetEmail = myEmail.equals(room.getBuyerEmail())
                            ? room.getSellerEmail()
                            : room.getBuyerEmail();
                    String targetNickname = userAccountRepository.findById(targetEmail)
                            .map(u -> u.getNickname())
                            .orElse(targetEmail);
                    return ChatRoomResponse.of(room, myEmail, targetNickname);
                })
                .collect(Collectors.toList());
    }

    /** 채팅방 생성 (이미 있으면 기존 방 반환) */
    @Transactional
    public ChatRoom findOrCreate(String buyerEmail, String sellerEmail, Long productId, String productName) {
        // [신규] 판매자(본인 상품 아닌 경우)·관리자는 문의(채팅) 시작 불가
        String buyerRole = userAccountRepository.findById(buyerEmail)
                .map(u -> u.getRole())
                .orElse(null);
        boolean isRestrictedRole = "ROLE_SELLER".equals(buyerRole) || "ROLE_ADMIN".equals(buyerRole);
        if (isRestrictedRole && !buyerEmail.equals(sellerEmail)) {
            throw new CustomException(ErrorCode.AUTH_ACCESS_DENIED);
        }

        return chatRoomRepository
                .findByBuyerEmailAndSellerEmailAndProductId(buyerEmail, sellerEmail, productId)
                .orElseGet(() -> {
                    // [MEM-RQ-002] 판매자가 탈퇴한 상품은 새 채팅방(문의) 생성 불가.
                    // 기존 대화(이미 찾은 방)는 CHAT-RQ-001에 따라 계속 조회 가능해야 하므로 신규 생성 분기에서만 막는다.
                    boolean sellerWithdrawn = productRepository.findById(productId)
                            .map(p -> "SELLER_WITHDRAWN".equals(p.getStatus()))
                            .orElse(false);
                    if (sellerWithdrawn) {
                        throw new CustomException(ErrorCode.PRODUCT_SELLER_WITHDRAWN);
                    }
                    return chatRoomRepository.save(
                            ChatRoom.create(buyerEmail, sellerEmail, productId, productName));
                });
    }

    /** 채팅방 조회 */
    @Transactional(readOnly = true)
    public ChatRoom getRoom(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    /** 메시지 전송 후 채팅방 마지막 메시지 업데이트 */
    @Transactional
    public void updateLastMessage(Long roomId, String senderEmail, String content) {
        ChatRoom room = getRoom(roomId);
        room.setLastMessage(content);
        room.setLastSentAt(java.time.LocalDateTime.now());

        // 상대방 안읽음 카운트 증가
        if (senderEmail.equals(room.getBuyerEmail())) {
            room.setSellerUnreadCount(room.getSellerUnreadCount() + 1);
        } else {
            room.setBuyerUnreadCount(room.getBuyerUnreadCount() + 1);
        }
    }

    /** 읽음 처리 — 내 안읽음 카운트 초기화 */
    @Transactional
    public void resetUnreadCount(Long roomId, String myEmail) {
        ChatRoom room = getRoom(roomId);
        if (myEmail.equals(room.getBuyerEmail())) {
            room.setBuyerUnreadCount(0);
        } else {
            room.setSellerUnreadCount(0);
        }
    }
}
