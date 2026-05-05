package com.demo.gp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ADMIN_LOG")
public class AdminLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    private String scenario;  // 시나리오A / 시나리오B

    private String action;    // 발주확정 / 실패확정 / 마감연장

    @Column(length = 500)
    private String message;

    public Long getLogId()                        { return logId; }
    public void setLogId(Long logId)              { this.logId = logId; }

    public LocalDateTime getCreatedAt()           { return createdAt; }
    public void setCreatedAt(LocalDateTime t)     { this.createdAt = t; }

    public String getScenario()                   { return scenario; }
    public void setScenario(String scenario)      { this.scenario = scenario; }

    public String getAction()                     { return action; }
    public void setAction(String action)          { this.action = action; }

    public String getMessage()                    { return message; }
    public void setMessage(String message)        { this.message = message; }
}
