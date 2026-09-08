package com.Nbbang.backend.domain.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// [신규] PRD-RQ-004: 가격 변경 감사 이력 (변경 전/후 값, 판매자, 시각, 사유, 재기록된 온체인 해시 보존)
@Entity
@Table(name = "product_price_history")
@Getter @Setter
@NoArgsConstructor
public class ProductPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "old_price", nullable = false)
    private BigDecimal oldPrice;

    @Column(name = "new_price", nullable = false)
    private BigDecimal newPrice;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "changed_by", nullable = false)
    private String changedBy; // 변경한 판매자 이메일 (세션 기준)

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    private String reason;

    @Column(name = "new_data_hash")
    private String newDataHash;

    @Column(name = "new_tx_hash")
    private String newTxHash;

    @PrePersist
    protected void onCreate() {
        this.changedAt = LocalDateTime.now();
    }
}
