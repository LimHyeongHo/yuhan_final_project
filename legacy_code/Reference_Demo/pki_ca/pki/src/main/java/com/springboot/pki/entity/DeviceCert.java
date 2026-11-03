package com.springboot.pki.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pki_table")
@Getter
@Setter
public class DeviceCert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId; // user.email 참조

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

    // 패스워드는 user 테이블에 있으므로 여기서는 제거하거나 용도에 맞게 사용
    @Column
    private String password; 
}
