package com.demo.gp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "PARTICIPATION")
public class Participation {

    @Id
    @Column(name = "part_id")
    private Integer partId;

    @Column(name = "gp_id")
    private Integer gpId;

    @Column(name = "buyer_id")
    private Integer buyerId;     // USER 참조 (buyerName → buyerId로 변경)

    private int qty;             // 구매 수량

    @Column(name = "payment_status")
    private String paymentStatus;  // 완료 / 대기 / 실패 / 환불

    @Column(name = "final_price")
    private int finalPrice;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    public Integer getPartId()                       { return partId; }
    public void setPartId(Integer id)                { this.partId = id; }

    public Integer getGpId()                         { return gpId; }
    public void setGpId(Integer gpId)                { this.gpId = gpId; }

    public Integer getBuyerId()                      { return buyerId; }
    public void setBuyerId(Integer id)               { this.buyerId = id; }

    public int getQty()                              { return qty; }
    public void setQty(int qty)                      { this.qty = qty; }

    public String getPaymentStatus()                 { return paymentStatus; }
    public void setPaymentStatus(String status)      { this.paymentStatus = status; }

    public int getFinalPrice()                       { return finalPrice; }
    public void setFinalPrice(int finalPrice)        { this.finalPrice = finalPrice; }

    public LocalDateTime getAppliedAt()              { return appliedAt; }
    public void setAppliedAt(LocalDateTime t)        { this.appliedAt = t; }
}