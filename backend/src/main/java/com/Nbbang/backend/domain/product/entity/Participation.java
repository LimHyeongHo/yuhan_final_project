package com.Nbbang.backend.domain.product.entity;

import com.Nbbang.backend.domain.auth.entity.UserAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 실제 참여자 계정과의 연관관계. nullable인 이유: 기존 데이터(연관관계 도입 이전)에는 값이 없을 수 있어서
    // DB에 NOT NULL로 컬럼을 추가하면 기존 row 때문에 마이그레이션이 깨짐.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_email")
    private UserAccount member;

    @Column(nullable = false)
    private String buyerName; // 참여 시점 member 닉네임 스냅샷 (화면 표시용)

    @Column(nullable = false)
    private LocalDateTime joinDate;

    @PrePersist
    protected void onCreate() {
        this.joinDate = LocalDateTime.now();
    }
}
