package com.Nbbang.backend.domain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentPrepareResponse {
    private String orderId;
    private Long amount;
}
