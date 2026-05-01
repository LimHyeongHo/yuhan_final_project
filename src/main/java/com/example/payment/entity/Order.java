package com.example.payment.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "orders")
public class Order {

    @Id
    private String orderId;
    private String orderName;
    private Long totalAmount;
    private int totalParticipants;
    private int activeParticipants;
    private LocalDateTime deadline;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Participant> participants = new ArrayList<>();
}
