package com.demo.gp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "GROUP_PURCHASE")
public class GroupPurchase {

    @Id
    @Column(name = "gp_id")
    private Integer gpId;

    @Column(name = "product_id")
    private Integer productId;

    private String status;

    @Column(name = "current_qty")
    private int currentQty;

    @Column(name = "discount_rate")
    private int discountRate;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    // ✅ ERD에 없지만 비즈니스 로직상 필요 — min_qty만 추가
    @Column(name = "min_qty")
    private int minQty;

    public Integer getGpId()                         { return gpId; }
    public void setGpId(Integer gpId)                { this.gpId = gpId; }

    public Integer getProductId()                    { return productId; }
    public void setProductId(Integer id)             { this.productId = id; }

    public String getStatus()                        { return status; }
    public void setStatus(String status)             { this.status = status; }

    public int getCurrentQty()                       { return currentQty; }
    public void setCurrentQty(int qty)               { this.currentQty = qty; }

    public int getDiscountRate()                     { return discountRate; }
    public void setDiscountRate(int rate)            { this.discountRate = rate; }

    public LocalDateTime getEndAt()                  { return endAt; }
    public void setEndAt(LocalDateTime endAt)        { this.endAt = endAt; }

    public int getMinQty()                           { return minQty; }
    public void setMinQty(int minQty)                { this.minQty = minQty; }
}