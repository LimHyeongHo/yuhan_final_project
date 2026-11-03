package com.Nbbang.backend.domain.review.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 거래 후기 (같은 공동구매에는 한 사람당 하나만 쓸 수 있음)
// 후기는 상품을 조회만 해서 Product와 연관관계 없이 productId만 저장함
// 삭제하면 행을 아예 지워서 다시 쓸 수 있게 함
@Entity
@Table(name = "review", uniqueConstraints = {
        @UniqueConstraint(name = "uk_review_product_reviewer", columnNames = {"product_id", "reviewer_email"})
})
@Getter
@Setter
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "reviewer_email", nullable = false, length = 255)
    private String reviewerEmail;

    @Column(name = "seller_email", nullable = false, length = 255)
    private String sellerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Sentiment sentiment;

    @Column(length = 500)
    private String content; // 한 줄 후기 (선택 입력)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
