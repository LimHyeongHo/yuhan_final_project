package com.Nbbang.backend.domain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WithdrawableAmountResponse {
    private Long withdrawableAmount;
}
