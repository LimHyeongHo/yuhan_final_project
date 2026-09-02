package com.Nbbang.backend.domain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class WithdrawalHistoryResponse {
    private Long id;
    private Long amount;
    private String status;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
}
