package com.Nbbang.backend.domain.chat.controller;

import com.Nbbang.backend.domain.chat.dto.ChatMessageResponse;
import com.Nbbang.backend.domain.chat.dto.ChatRoomResponse;
import com.Nbbang.backend.domain.chat.entity.ChatRoom;
import com.Nbbang.backend.domain.chat.entity.MessageType;
import com.Nbbang.backend.domain.chat.service.ChatMessageService;
import com.Nbbang.backend.domain.chat.service.ChatRoomService;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 채팅방 REST API
 *
 * GET  /api/chat/rooms                        내 채팅방 목록
 * POST /api/chat/rooms                        채팅방 생성 (또는 기존 방 반환)
 * GET  /api/chat/rooms/{roomId}/messages      메시지 내역
 * POST /api/chat/rooms/{roomId}/read          읽음 처리
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final SimpMessageSendingOperations messagingTemplate;

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = List.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB

    /** 내 채팅방 목록 */
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomResponse>> getRooms(HttpSession session) {
        return ResponseEntity.ok(chatRoomService.getRooms(getEmail(session)));
    }

    /**
     * 채팅방 생성 or 기존 방 반환
     * 요청 body: { sellerEmail, productId, productName }
     */
    @PostMapping("/rooms")
    public ResponseEntity<Map<String, Long>> createRoom(
            @RequestBody Map<String, String> body,
            HttpSession session) {
        String buyerEmail = getEmail(session);
        String sellerEmail = body.get("sellerEmail");
        Long productId = Long.parseLong(body.get("productId"));
        String productName = body.get("productName");

        ChatRoom room = chatRoomService.findOrCreate(buyerEmail, sellerEmail, productId, productName);
        return ResponseEntity.ok(Map.of("roomId", room.getId()));
    }

    /** 메시지 내역 (최근 50개) */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            @PathVariable Long roomId,
            HttpSession session) {
        String myEmail = getEmail(session);
        ChatRoom room = chatRoomService.getRoom(roomId);
        validateAccess(room, myEmail);
        return ResponseEntity.ok(chatMessageService.getHistory(room, myEmail));
    }

    /** 읽음 처리 + WebSocket READ 이벤트 브로드캐스트 */
    @PostMapping("/rooms/{roomId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long roomId, HttpSession session) {
        String myEmail = getEmail(session);
        ChatRoom room = chatRoomService.getRoom(roomId);
        validateAccess(room, myEmail);
        chatMessageService.markAsRead(roomId, myEmail);
        chatRoomService.resetUnreadCount(roomId, myEmail);
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, ChatMessageResponse.readEvent(roomId, myEmail));
        // 내 개인 토픽에도 알림 → 다른 탭·기기의 헤더 배지도 이 방을 읽음 처리
        messagingTemplate.convertAndSend("/topic/chat/user/" + myEmail, ChatMessageResponse.readEvent(roomId, myEmail));
        return ResponseEntity.ok().build();
    }

    /**
     * [CHAT-RQ-001] 채팅방 나가기 (카톡 방식)
     * 본인 방에서만 가능(타인 대리 나가기 → 403). 나가면 "OO님이 나가셨습니다" 시스템 메시지를 양쪽에 브로드캐스트한다.
     */
    @PostMapping("/rooms/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable Long roomId, HttpSession session) {
        String myEmail = getEmail(session);
        ChatRoom room = chatRoomService.getRoom(roomId);
        validateAccess(room, myEmail);
        if (chatRoomService.leaveRoom(roomId, myEmail)) {
            broadcastSystemMessage(room, myEmail, MessageType.LEAVE);
        }
        return ResponseEntity.ok().build();
    }

    /**
     * [CHAT-RQ-001] 채팅방 재입장
     * 나가 있던 사용자가 방을 다시 열 때 호출. "OO님이 들어왔습니다" 시스템 메시지를 양쪽에 브로드캐스트한다.
     */
    @PostMapping("/rooms/{roomId}/rejoin")
    public ResponseEntity<Void> rejoinRoom(@PathVariable Long roomId, HttpSession session) {
        String myEmail = getEmail(session);
        ChatRoom room = chatRoomService.getRoom(roomId);
        validateAccess(room, myEmail);
        if (chatRoomService.rejoinRoom(roomId, myEmail)) {
            broadcastSystemMessage(room, myEmail, MessageType.JOIN);
        }
        return ResponseEntity.ok().build();
    }

    /** 나가기/재입장 시스템 메시지를 저장하고 방 토픽 + 양측 개인 토픽에 발행 */
    private void broadcastSystemMessage(ChatRoom room, String actorEmail, MessageType type) {
        ChatMessageResponse sysMsg = chatMessageService.saveSystemMessage(room.getId(), actorEmail, type);
        chatRoomService.updateLastMessagePreview(room.getId(), sysMsg.getContent());
        messagingTemplate.convertAndSend("/topic/chat/" + room.getId(), sysMsg);
        messagingTemplate.convertAndSend("/topic/chat/user/" + room.getBuyerEmail(), sysMsg);
        messagingTemplate.convertAndSend("/topic/chat/user/" + room.getSellerEmail(), sysMsg);
    }

    /**
     * [CHAT-RQ-002] 메시지 전송 취소 (소프트 삭제)
     * 작성자 본인만 가능(타인 요청 → 403), 취소 이벤트를 WebSocket으로 양측에 즉시 브로드캐스트한다.
     * 마지막 메시지를 취소하면 채팅방 목록 미리보기도 함께 갱신한다.
     */
    @DeleteMapping("/rooms/{roomId}/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            HttpSession session) {
        String myEmail = getEmail(session);
        ChatRoom room = chatRoomService.getRoom(roomId);
        validateAccess(room, myEmail);
        String preview = chatMessageService.deleteMessage(roomId, messageId, myEmail);
        chatRoomService.updateLastMessagePreview(roomId, preview);
        ChatMessageResponse deleteEvent = ChatMessageResponse.deleteEvent(roomId, messageId, preview);
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, deleteEvent);
        // 양측 개인 토픽에도 발행 → 채팅 화면 밖에서도 헤더 목록 미리보기가 갱신되도록
        messagingTemplate.convertAndSend("/topic/chat/user/" + room.getBuyerEmail(), deleteEvent);
        messagingTemplate.convertAndSend("/topic/chat/user/" + room.getSellerEmail(), deleteEvent);
        return ResponseEntity.ok().build();
    }

    /** 채팅 이미지 업로드 — 저장 후 접근 URL 반환 (상품 이미지 업로드와 동일한 로컬 저장 방식) */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("image") MultipartFile image,
            HttpSession session,
            HttpServletRequest request) {
        getEmail(session); // 로그인 안 된 경우 401

        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new CustomException(ErrorCode.CHAT_FILE_SIZE_EXCEEDED);
        }

        String originalFilename = image.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                : "";
        String contentType = image.getContentType();
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension) || contentType == null || !contentType.startsWith("image/")) {
            throw new CustomException(ErrorCode.CHAT_INVALID_FILE_FORMAT);
        }

        String uploadDir = System.getProperty("user.dir") + "/uploads/";
        try {
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String savedFilename = UUID.randomUUID().toString() + extension;

            Path filePath = Paths.get(uploadDir + savedFilename);
            Files.write(filePath, image.getBytes());

            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            return ResponseEntity.ok(Map.of("url", baseUrl + "/uploads/" + savedFilename));
        } catch (IOException e) {
            throw new RuntimeException("이미지 업로드 실패: " + e.getMessage());
        }
    }

    /** 세션에서 userId(email) 추출 — 로그인 안 된 경우 401 */
    private String getEmail(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        return userId;
    }

    private void validateAccess(ChatRoom room, String myEmail) {
        if (!myEmail.equals(room.getBuyerEmail()) && !myEmail.equals(room.getSellerEmail())) {
            throw new CustomException(ErrorCode.CHAT_ACCESS_DENIED);
        }
    }
}
