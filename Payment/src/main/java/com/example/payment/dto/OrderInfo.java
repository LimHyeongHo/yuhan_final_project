package com.example.payment.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Data
public class OrderInfo {
    private String orderId;
    private Long totalAmount;
    private int totalParticipants;
    private int activeParticipants;
    private LocalDateTime deadline;
    private List<ParticipantInfo> participants = new ArrayList<>();

    @Data
    public static class ParticipantInfo {
        private int id;
        private String name;
        private boolean cancelled;
        private boolean unpaid; // 미입금 여부
    }
}