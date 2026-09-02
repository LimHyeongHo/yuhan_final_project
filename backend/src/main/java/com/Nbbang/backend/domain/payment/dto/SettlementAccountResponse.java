package com.Nbbang.backend.domain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SettlementAccountResponse {
    private String bankName;
    private String accountNumber;
    private String accountHolder;
}
