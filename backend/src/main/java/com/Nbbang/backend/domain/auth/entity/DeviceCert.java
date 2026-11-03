package com.Nbbang.backend.domain.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "pki_table")
@Getter
@Setter
public class DeviceCert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String ciHash;

    @Column(columnDefinition = "TEXT")
    private String publicKey;

    @Column(nullable = false, unique = true)
    private String deviceId;

    @Column
    private String certificateSerialNumber;

    @Column
    private boolean revoked = false;

    // CAService가 발급하는 X.509 기기 인증서의 실제 유효기간(발급 시점 ~ +1년)
    @Column(name = "certificate_issued_at")
    private LocalDateTime certificateIssuedAt;

    @Column(name = "certificate_expires_at")
    private LocalDateTime certificateExpiresAt;
}
