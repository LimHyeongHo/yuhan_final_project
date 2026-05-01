package com.example.payment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String paymentKey;
    private String orderId;
    private String orderName;
    private Long totalAmount;
    private Long perPerson;
    private String status;
    private String method;
    private LocalDateTime paidAt;
}
