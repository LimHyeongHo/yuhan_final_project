package com.Nbbang.backend.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_account")
@Getter
@Setter
public class UserAccount {
    @Id
    @Column(length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String role = "ROLE_USER";

    // MEM-RQ-001: 탈퇴 시 물리 삭제 대신 이 값을 "WITHDRAWN"으로 바꿔 비활성화한다 (거래 감사 이력 보존).
    // columnDefinition에 DB 기본값을 명시해야 함 - 이미 데이터가 있는 테이블에 NOT NULL 컬럼을
    // "default 없이" 추가하면 기존 행이 전부 NULL이라 Postgres가 ALTER TABLE 자체를 거부한다.
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'ACTIVE'")
    private String status = "ACTIVE";

    // 가입 일시. 최초 저장 시 한 번만 기록되고 이후 변경되지 않음(updatable = false).
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
