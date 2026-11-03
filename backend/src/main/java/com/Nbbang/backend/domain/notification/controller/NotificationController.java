package com.Nbbang.backend.domain.notification.controller;

import com.Nbbang.backend.domain.notification.service.NotificationService;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<?> getMyNotifications(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        
        String email = (String) session.getAttribute("userId");
        List<Map<String, Object>> notifications = notificationService.getNotificationsForUser(email);
        return ResponseEntity.ok(notifications);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(HttpServletRequest request, @org.springframework.web.bind.annotation.PathVariable Long id) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        
        String email = (String) session.getAttribute("userId");
        notificationService.deleteNotification(id, email);
        return ResponseEntity.ok("알림이 삭제되었습니다.");
    }
}
