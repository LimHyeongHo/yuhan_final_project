package com.example.payment.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentRequest {

    private String paymentKey;
    private String orderId;
    private Long amount;
    private LocalDateTime deadline;
    private String orderName;
    private int participants;
}
