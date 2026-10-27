package com.Nbbang.backend.domain.chat.service;

import com.Nbbang.backend.domain.auth.entity.UserAccount;
import com.Nbbang.backend.domain.auth.repository.UserAccountRepository;
import com.Nbbang.backend.domain.chat.dto.ChatRoomResponse;
import com.Nbbang.backend.domain.chat.entity.ChatRoom;
import com.Nbbang.backend.domain.chat.repository.ChatRoomRepository;
import com.Nbbang.backend.domain.product.entity.Product;
import com.Nbbang.backend.domain.product.repository.ProductRepository;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserAccountRepository userAccountRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getRooms(String myEmail) {
        List<ChatRoom> rooms = chatRoomRepository
                .findByBuyerEmailOrSellerEmailOrderByLastSentAtDesc(myEmail, myEmail);

        /// 구매자가 나간 방은 목록에서 빼고, 판매자가 나간 방은 그대로 둠
        return rooms.stream()
                .filter(room -> !(myEmail.equals(room.getBuyerEmail()) && room.getBuyerLeftAt() != null))
                .sorted(Comparator.comparing(ChatRoomService::lastActivityAt).reversed())
                .map(room -> {
                    String targetEmail = myEmail.equals(room.getBuyerEmail())
                            ? room.getSellerEmail()
                            : room.getBuyerEmail();
                    Optional<UserAccount> targetAccount = userAccountRepository.findById(targetEmail);
                    String targetNickname = targetAccount.map(UserAccount::getNickname).orElse(targetEmail);
                    /// 상대가 탈퇴했는지 확인
                    boolean targetWithdrawn = targetAccount.map(u -> "WITHDRAWN".equals(u.getStatus())).orElse(false);
                    /// 연결된 상품의 삭제 여부와 현재 상태 확인
                    Optional<Product> product = room.getProductId() != null
                            ? productRepository.findById(room.getProductId())
                            : Optional.empty();
                    boolean productDeleted = room.getProductId() != null && product.isEmpty();
                    String productStatus = product.map(Product::getStatus).orElse(null);
                    String productImageUrl = product.map(Product::getImageUrl).orElse(null);
                    return ChatRoomResponse.of(room, myEmail, targetNickname, targetWithdrawn, productDeleted,
                            productStatus, productImageUrl);
                })
                .collect(Collectors.toList());
    }

    /// 목록 정렬 기준 (마지막 메시지 시각, 없으면 방 만든 시각)
    private static LocalDateTime lastActivityAt(ChatRoom room) {
        if (room.getLastSentAt() != null) return room.getLastSentAt();
        return room.getCreatedAt() != null ? room.getCreatedAt() : LocalDateTime.MIN;
    }

    /// 채팅방 생성 (같은 구매자, 판매자, 상품이면 기존 방 반환)
    @Transactional
    public ChatRoom findOrCreate(String buyerEmail, String sellerEmail, Long productId) {
        /// 판매자, 관리자는 다른 판매자 상품에 문의 불가 (구매자만 문의 가능)
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
                    /// 나갔던 구매자가 다시 문의하면 같은 방 사용
                    /// 나간 시각 기준선은 그대로 둬서 이전 메시지는 계속 안 보임
                    if (room.getBuyerLeftAt() != null) {
                        room.setBuyerLeftAt(null);
                        room.setBuyerUnreadCount(0);
                    }
                    return room;
                })
                .orElseGet(()-> {
                    /// 새 방을 만들 때 요청한 판매자가 실제 상품 판매자인지 확인 (요청 값 위조 방지)
                    Product product = productRepository.findById(productId)
                            .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
                    if (!product.getSellerEmail().equals(sellerEmail)) {
                        throw new CustomException(ErrorCode.CHAT_ROOM_SELLER_MISMATCH);
                    }

                    /// 판매자가 탈퇴한 상품은 새 문의 불가 (기존 대화는 계속 볼 수 있음)
                    if ("SELLER_WITHDRAWN".equals(product.getStatus())) {
                        throw new CustomException(ErrorCode.PRODUCT_SELLER_WITHDRAWN);
                    }
                    return chatRoomRepository.save(
                            /// 상품명은 클라이언트 값을 믿지 않고 서버가 DB에서 채움
                            ChatRoom.create(buyerEmail, sellerEmail, productId, product.getTitle()));
                });
    }

    /// 채팅방 나가기 (이미 나갔으면 false)
    /// 구매자는 이전 메시지 복구 불가, 판매자는 나간 시각만 기록
    @Transactional
    public boolean leaveRoom(Long roomId, String myEmail) {
        ChatRoom room = getRoom(roomId);
        LocalDateTime now = LocalDateTime.now();
        if (myEmail.equals(room.getBuyerEmail())) {
            if (room.getBuyerLeftAt() != null) return false;
            room.setBuyerLeftAt(now);
            room.setBuyerHistoryFrom(now); /// 이 시각 이전 메시지는 다시 들어와도 안 보이게 기록
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

    /// 채팅방 재입장 (판매자만, 구매자는 재입장 없음)
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

    @Transactional(readOnly = true)
    public ChatRoom getRoom(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    /// 메시지 전송 후 목록 미리보기와 상대의 안 읽은 수 갱신
    @Transactional
    public void updateLastMessage(Long roomId, String senderEmail, String content) {
        ChatRoom room = getRoom(roomId);
        room.setLastMessage(content);
        room.setLastSentAt(java.time.LocalDateTime.now());

        /// 상대가 방에 있을 때만 상대의 안 읽은 수 +1 (나간 사람은 알림 안 쌓임)
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

    /// 마지막 메시지 삭제 시 미리보기 문구만 바꿈 (목록 순서는 유지)
    @Transactional
    public void updateLastMessagePreview(Long roomId, String preview) {
        ChatRoom room = getRoom(roomId);
        room.setLastMessage(preview != null ? preview : "메시지가 삭제되었습니다");
    }

    /// 내 안 읽은 수 0으로 초기화
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
