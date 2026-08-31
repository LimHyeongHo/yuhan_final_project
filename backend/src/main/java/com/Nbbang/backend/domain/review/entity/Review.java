package com.Nbbang.backend.domain.review.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 거래 후기.
 *
 * Product 와 연관관계를 걸지 않는다 — 후기 도메인은 Product 를 조회만 하고 소유하지 않기 때문에
 * productId 를 단순 Long 으로만 들고 있는다(domain/payment 의 Payment.productId 와 동일한 방식).
 *
 * 한 사람이 같은 공동구매에 후기를 두 번 쓰지 못하도록 (product_id, reviewer_email) 유니크 제약.
 * 후기 삭제는 soft-delete 가 아니라 row 자체를 지워서 재작성이 가능하도록 한다.
 */
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
    private String content; // 한 줄 후기, 선택 입력이라 nullable

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
