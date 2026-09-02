package com.Nbbang.backend.domain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AdminWithdrawalResponse {
    private Long id;
    private String sellerEmail;
    private Long amount;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private String status;
    private LocalDateTime requestedAt;
}
