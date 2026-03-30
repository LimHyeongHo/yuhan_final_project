package com.springboot.pki.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String userId; // 로그인용 아이디

    @Column(unique = true, nullable = false)
    private String ciHash; // HMAC(CI, ServerSecret)

    @Column(nullable = false)
    private String password;

    @Column(columnDefinition = "TEXT")
    private String publicKey; // RSA Public Key (Base64 encoded)

    @Column(nullable = false)
    private String deviceId; // 기기 바인딩용
}
