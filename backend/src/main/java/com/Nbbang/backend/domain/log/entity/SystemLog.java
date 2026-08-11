package com.Nbbang.backend.domain.log.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Getter
@NoArgsConstructor
public class SystemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // UUID or custom ID format (like "TX-99824-B") for display purposes
    private String displayId;

    private String type; // SECURITY, MEMBER, PRODUCT
    
    private String status; // SUCCESS, TAMPERED, FAILED
    
    private String detail; // e.g. "상세 정보", "위변조 추적"
    
    private String diff; // e.g. "0x0000...0000"

    private String timestamp;

    @Builder
    public SystemLog(String displayId, String type, String status, String detail, String diff) {
        this.displayId = displayId;
        this.type = type;
        this.status = status;
        this.detail = detail;
        this.diff = diff;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
    }
}
