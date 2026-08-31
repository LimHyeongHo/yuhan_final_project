package com.Nbbang.backend.domain.notification.service;

import com.Nbbang.backend.domain.notification.entity.Notification;
import com.Nbbang.backend.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getNotificationsForUser(String userEmail) {
        return notificationRepository.findByUserEmailOrderByCreatedAtDesc(userEmail)
                .stream()
                .map(notif -> Map.of(
                        "id", (Object) notif.getId(),
                        "message", notif.getMessage(),
                        "isRead", notif.isRead(),
                        "createdAt", notif.getCreatedAt().toString()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteNotification(Long id, String userEmail) {
        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 알림입니다."));
        
        if (!notification.getUserEmail().equals(userEmail)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        
        notificationRepository.delete(notification);
    }
}
