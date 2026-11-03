package com.Nbbang.backend.domain.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "withdrawal_request")
@Getter @Setter
@NoArgsConstructor
public class WithdrawalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_email", nullable = false)
    private String sellerEmail;

    @Column(nullable = false)
    private Long amount;

    // REQUESTED → COMPLETED / REJECTED
    @Column(nullable = false, length = 20)
    private String status = "REQUESTED";

    // 신청 시점 계좌 정보
    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "account_holder", nullable = false)
    private String accountHolder;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "requested_at", updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        this.requestedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "REQUESTED";
        }
    }
}
