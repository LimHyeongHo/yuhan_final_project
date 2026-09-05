package com.Nbbang.backend.domain.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product")
@Getter @Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    // 💡 로그인 담당 팀원이 User 엔티티를 만들 때까지 임시로 Long 타입을 사용합니다.
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    // [신규] POST /api/chat/rooms 호출용 판매자 이메일
    @Column(name = "seller_email")
    private String sellerEmail;

    @Column(nullable = false, length = 20)
    private String type; // 'BOOK' or 'ITEM'

    @Column(length = 100)
    private String category; // 도서 또는 물품 카테고리

    @Column(nullable = false)
    private String title;

    @Column(length = 100)
    private String author; // ITEM의 경우 없을 수 있음

    @Column(length = 100)
    private String publisher;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    // [블록체인 연동]
    @Column(name = "isbn", length = 20)
    private String isbn; // 알라딘 정가 조회용 ISBN

    @Column(name = "aladdin_price")
    private BigDecimal aladdinPrice; // 알라딘 조회 정가 (캐싱용)

    @Column(name = "tx_hash", length = 100)
    private String txHash; // 블록체인 스마트 컨트랙트에 기록된 트랜잭션 해시

    @Enumerated(EnumType.STRING)
    @Column(name = "blockchain_status", length = 30)
    private BlockchainJobStatus blockchainStatus = BlockchainJobStatus.QUEUED;

    @Column(name = "blockchain_retry_count", nullable = false)
    private Integer blockchainRetryCount = 0;

    @Column(name = "blockchain_last_error", columnDefinition = "TEXT")
    private String blockchainLastError;

    @Column(name = "blockchain_updated_at")
    private LocalDateTime blockchainUpdatedAt;

    @Column(name = "original_price")
    private BigDecimal originalPrice; // 프론트에서 정가 정보가 넘어올 경우 대비, 기본은 price 와 같게 처리

    @Column(name = "price", nullable = false)
    private BigDecimal price; // 공동구매 가격

    @Column(name = "target_count", nullable = false)
    private Integer targetCount; // 목표 인원

    @Column(name = "current_count", nullable = false)
    private Integer currentCount = 0; // 현재 참여 인원

    @Column(nullable = false)
    private LocalDateTime deadline; // 마감 기한

    @Column(nullable = false, length = 50)
    private String status = "OPEN"; // 기본값으로 'OPEN'(모집중) 설정

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Participation> participations = new java.util.ArrayList<>();

    // 데이터가 처음 저장될 때 시간 및 기한 자동 설정
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.deadline == null) {
            this.deadline = LocalDateTime.now().plusDays(14); // 마감일 기본값 14일 뒤로 설정
        }
        if (this.currentCount == null) {
            this.currentCount = 0;
        }
        if (this.blockchainStatus == null) {
            this.blockchainStatus = BlockchainJobStatus.QUEUED;
        }
        if (this.blockchainRetryCount == null) {
            this.blockchainRetryCount = 0;
        }
        if (this.blockchainUpdatedAt == null) {
            this.blockchainUpdatedAt = LocalDateTime.now();
        }
    }

    // 데이터가 수정될 때 시간 자동 갱신
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 공동구매 참여 인원 증가 로직
    public void incrementCurrentCount() {
        if (this.currentCount == null) {
            this.currentCount = 0;
        }
        if (this.currentCount < this.targetCount) {
            this.currentCount++;
        }
    }

    // 공동구매 참여 인원 감소 로직
    public void decrementCurrentCount() {
        if (this.currentCount != null && this.currentCount > 0) {
            this.currentCount--;
        }
    }
}
