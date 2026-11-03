package com.Nbbang.backend.domain.payment.dto;

import lombok.Data;

@Data
public class SettlementAccountRequest {
    private String bankName;
    private String accountNumber;
    private String accountHolder;
}
