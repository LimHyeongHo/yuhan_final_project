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

    // REQUESTED -> COMPLETED 또는 REJECTED (관리자 수동 승인/거절, 실제 송금 자동화는 없음)
    @Column(nullable = false, length = 20)
    private String status = "REQUESTED";

    // 신청 시점의 정산 계좌 스냅샷 (이후 계좌를 바꿔도 이 신청 건의 입금 정보는 안 바뀌어야 함)
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
