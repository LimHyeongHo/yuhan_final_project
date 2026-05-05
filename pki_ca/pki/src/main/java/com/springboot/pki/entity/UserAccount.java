package com.springboot.pki.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user") // 기존 user 테이블
@Getter
@Setter
public class UserAccount {
    @Id
    @Column(length = 100)
    private String email; // USER_ID가 참조하는 PK

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String role = "ROLE_USER"; // 기본값 설정
}
