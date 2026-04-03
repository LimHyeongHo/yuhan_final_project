package com.example.payment.dto;

import lombok.Data;

@Data
public class PaymentResponse {
    private String paymentKey;
    private String orderId;
    private Long totalAmount;
    private String status;
    private String method;
}