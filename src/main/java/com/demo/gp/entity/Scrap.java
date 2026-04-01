package com.demo.gp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// 데모용 — 기능 구현 생략, Entity만 선언
@Entity
@Table(name = "SCRAPS")
public class Scrap {

    @Id
    @Column(name = "scrap_id")
    private Integer scrapId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Integer getScrapId()                      { return scrapId; }
    public void setScrapId(Integer id)               { this.scrapId = id; }

    public Integer getUserId()                       { return userId; }
    public void setUserId(Integer id)                { this.userId = id; }

    public Integer getProductId()                    { return productId; }
    public void setProductId(Integer id)             { this.productId = id; }

    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime t)        { this.createdAt = t; }
}
