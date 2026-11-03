package com.Nbbang.backend.domain.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter @Setter
@NoArgsConstructor
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false)
    private String userEmail; // 수신자 (판매자 등)

    @Column(nullable = false, length = 1000)
    private String message; // 알림 내용

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Notification(String userEmail, String message) {
        this.userEmail = userEmail;
        this.message = message;
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
    }
}
