package com.Nbbang.backend.domain.payment.entity;

import com.Nbbang.backend.domain.auth.entity.UserAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter @Setter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(name = "payment_key", unique = true)
    private String paymentKey;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_email")
    private UserAccount member;

    @Column(name = "buyer_name", nullable = false)
    private String buyerName;

    @Column(nullable = false)
    private Long amount;

    // PENDING → APPROVED → DONE / 환불: CANCEL_REQUESTED → CANCELED 또는 REFUND_FAILED
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "cancel_requested_at")
    private LocalDateTime cancelRequestedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "PENDING";
        }
    }
}
