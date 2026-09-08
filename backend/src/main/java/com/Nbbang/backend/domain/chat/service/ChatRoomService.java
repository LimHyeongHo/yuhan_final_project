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

import java.time.LocalDateTime;
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

        // [CHAT-RQ-001] 구매자가 나간 방은 목록에서 제외 (판매자 나간 방은 유지 — 방 열면 재입장)
        return rooms.stream()
                .filter(room -> !(myEmail.equals(room.getBuyerEmail()) && room.getBuyerLeftAt() != null))
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
                .map(room -> {
                    // [CHAT-RQ-001] 나갔던 구매자가 재문의하면 같은 방 반환 (buyerHistoryFrom 유지 → 이전 메시지는 안 보임)
                    if (room.getBuyerLeftAt() != null) {
                        room.setBuyerLeftAt(null);
                        room.setBuyerUnreadCount(0);
                    }
                    return room;
                })
                .orElseGet(()-> {
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

    /**
     * [CHAT-RQ-001] 채팅방 나가기. 구성원 아니면 403, 이미 나가 있으면 false (멱등).
     * 구매자는 buyerHistoryFrom 기준선을 찍어 이후 이 방의 이전 메시지를 볼 수 없다(복구 불가). 판매자는 나간 시각만 기록.
     * @return 이번 호출로 실제 나간 경우 true
     */
    @Transactional
    public boolean leaveRoom(Long roomId, String myEmail) {
        ChatRoom room = getRoom(roomId);
        LocalDateTime now = LocalDateTime.now();
        if (myEmail.equals(room.getBuyerEmail())) {
            if (room.getBuyerLeftAt() != null) return false;
            room.setBuyerLeftAt(now);
            room.setBuyerHistoryFrom(now); // 복구 불가 기준선
            room.setBuyerUnreadCount(0);
            return true;
        } else if (myEmail.equals(room.getSellerEmail())) {
            if (room.getSellerLeftAt() != null) return false;
            room.setSellerLeftAt(now);
            room.setSellerUnreadCount(0);
            return true;
        }
        throw new CustomException(ErrorCode.CHAT_ACCESS_DENIED);
    }

    /**
     * [CHAT-RQ-001] 채팅방 재입장 — 판매자 전용 (나간 판매자가 방을 다시 열 때). 구매자는 복구 불가라 no-op.
     * @return 이번 호출로 실제 재입장한 경우 true, 원래 나가 있지 않았으면 false (멱등)
     */
    @Transactional
    public boolean rejoinRoom(Long roomId, String myEmail) {
        ChatRoom room = getRoom(roomId);
        if (myEmail.equals(room.getSellerEmail())) {
            if (room.getSellerLeftAt() == null) return false;
            room.setSellerLeftAt(null);
            return true;
        }
        if (myEmail.equals(room.getBuyerEmail())) {
            return false;
        }
        throw new CustomException(ErrorCode.CHAT_ACCESS_DENIED);
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
        // [CHAT-RQ-001] 단, 상대가 이미 나간 방이면 알림(미읽음)을 올리지 않는다.
        boolean senderIsBuyer = senderEmail.equals(room.getBuyerEmail());
        if (senderIsBuyer) {
            if (room.getSellerLeftAt() == null) {
                room.setSellerUnreadCount(room.getSellerUnreadCount() + 1);
            }
        } else {
            if (room.getBuyerLeftAt() == null) {
                room.setBuyerUnreadCount(room.getBuyerUnreadCount() + 1);
            }
        }
    }

    /** [CHAT-RQ-002] 마지막 메시지 취소 시 채팅방 목록 미리보기 문구만 갱신 (정렬 시각은 건드리지 않음) */
    @Transactional
    public void updateLastMessagePreview(Long roomId, String preview) {
        ChatRoom room = getRoom(roomId);
        room.setLastMessage(preview != null ? preview : "메시지가 삭제되었습니다");
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
