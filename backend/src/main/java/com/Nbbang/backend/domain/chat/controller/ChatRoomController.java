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
import lombok.extern.slf4j.Slf4j;
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

/// 메시지 전송 외 채팅 기능은 REST API로 처리
/// 처리 결과는 웹소켓으로 상대 화면에 바로 반영
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final SimpMessageSendingOperations messagingTemplate;

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = List.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; /// 5MB

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomResponse>> getRooms(HttpSession session) {
        return ResponseEntity.ok(chatRoomService.getRooms(getEmail(session)));
    }

    /// 같은 구매자, 판매자, 상품이면 기존 방 재사용
    @PostMapping("/rooms")
    public ResponseEntity<Map<String, Long>> createRoom(
            @RequestBody Map<String, String> body,
            HttpSession session) {
        String buyerEmail = getEmail(session);
        String sellerEmail = body.get("sellerEmail");
        Long productId = Long.parseLong(body.get("productId"));

        ChatRoom room = chatRoomService.findOrCreate(buyerEmail, sellerEmail, productId);
        return ResponseEntity.ok(Map.of("roomId", room.getId()));
    }

    /// 최근 메시지 50개 조회
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            @PathVariable Long roomId,
            HttpSession session) {
        String myEmail = getEmail(session);
        ChatRoom room = chatRoomService.getRoom(roomId);
        validateAccess(room, myEmail);
        return ResponseEntity.ok(chatMessageService.getHistory(room, myEmail));
    }

    /// 읽음 처리 후 상대 화면의 '1' 표시 실시간 제거
    @PostMapping("/rooms/{roomId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long roomId, HttpSession session) {
        String myEmail = getEmail(session);
        ChatRoom room = chatRoomService.getRoom(roomId);
        validateAccess(room, myEmail);
        chatMessageService.markAsRead(roomId, myEmail);
        chatRoomService.resetUnreadCount(roomId, myEmail);
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, ChatMessageResponse.readEvent(roomId, myEmail));
        /// 다른 탭, 기기의 알림 배지도 같이 갱신
        messagingTemplate.convertAndSend("/topic/chat/user/" + myEmail, ChatMessageResponse.readEvent(roomId, myEmail));
        return ResponseEntity.ok().build();
    }

    /// 채팅방 나가기
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

    /// 나갔던 채팅방 다시 들어오기
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

    /// 입장, 퇴장 안내 메시지를 저장하고 양쪽에 전달
    private void broadcastSystemMessage(ChatRoom room, String actorEmail, MessageType type) {
        ChatMessageResponse sysMsg = chatMessageService.saveSystemMessage(room.getId(), actorEmail, type);
        chatRoomService.updateLastMessagePreview(room.getId(), sysMsg.getContent());
        messagingTemplate.convertAndSend("/topic/chat/" + room.getId(), sysMsg);
        messagingTemplate.convertAndSend("/topic/chat/user/" + room.getBuyerEmail(), sysMsg);
        messagingTemplate.convertAndSend("/topic/chat/user/" + room.getSellerEmail(), sysMsg);
    }

    /// 메시지는 실제로 지우지 않고 삭제 표시만 (소프트 삭제)
    @DeleteMapping("/rooms/{roomId}/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            HttpSession session) {
        String myEmail = getEmail(session);
        ChatRoom room = chatRoomService.getRoom(roomId);
        validateAccess(room, myEmail);
        ChatMessageService.DeleteMessageResult result = chatMessageService.deleteMessage(roomId, messageId, myEmail);
        /// 삭제한 게 마지막 메시지면 목록 미리보기도 갱신
        if (result.previewChanged()) {
            chatRoomService.updateLastMessagePreview(roomId, result.newPreview());
        }
        ChatMessageResponse deleteEvent = ChatMessageResponse.deleteEvent(roomId, messageId, result.previewChanged(), result.newPreview());
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, deleteEvent);
        messagingTemplate.convertAndSend("/topic/chat/user/" + room.getBuyerEmail(), deleteEvent);
        messagingTemplate.convertAndSend("/topic/chat/user/" + room.getSellerEmail(), deleteEvent);
        return ResponseEntity.ok().build();
    }

    /// 채팅 이미지 업로드 (서버 uploads 폴더에 저장)
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("image") MultipartFile image,
            HttpSession session,
            HttpServletRequest request) {
        getEmail(session); /// 로그인 확인

        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new CustomException(ErrorCode.CHAT_FILE_SIZE_EXCEEDED);
        }

        String originalFilename = image.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                : "";
        String contentType = image.getContentType();
        /// 확장자와 파일 형식이 모두 이미지인지 검사
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension) || contentType == null || !contentType.startsWith("image/")) {
            throw new CustomException(ErrorCode.CHAT_INVALID_FILE_FORMAT);
        }

        String uploadDir = System.getProperty("user.dir") + "/uploads/";
        try {
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            /// 파일명은 UUID로 바꿔 저장 (덮어쓰기를 방지함)
            String savedFilename = UUID.randomUUID().toString() + extension;

            Path filePath = Paths.get(uploadDir + savedFilename);
            Files.write(filePath, image.getBytes());

            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            return ResponseEntity.ok(Map.of("url", baseUrl + "/uploads/" + savedFilename));
        } catch (IOException e) {
            /// 내부 에러 내용은 숨기고 서버 로그에만 기록
            log.error("채팅 이미지 업로드 실패 — uploadDir: {}", uploadDir, e);
            throw new CustomException(ErrorCode.CHAT_IMAGE_UPLOAD_FAILED);
        }
    }

    /// 세션에 저장된 로그인 이메일 꺼내기
    private String getEmail(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        return userId;
    }

    /// 채팅방 구매자, 판매자만 접근 가능
    private void validateAccess(ChatRoom room, String myEmail) {
        if (!myEmail.equals(room.getBuyerEmail()) && !myEmail.equals(room.getSellerEmail())) {
            throw new CustomException(ErrorCode.CHAT_ACCESS_DENIED);
        }
    }
}
