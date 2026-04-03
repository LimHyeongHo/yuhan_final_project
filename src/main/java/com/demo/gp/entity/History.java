package com.demo.gp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "HISTORY")
public class History {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Integer historyId;

    @Column(name = "buyer_id")
    private Integer buyerId;

    @Column(name = "seller_id")
    private Integer sellerId;

    private String status;   // 구매완료 / 환불

    private String title;

    @Column(name = "final_price")
    private int finalPrice;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Integer getHistoryId()                    { return historyId; }
    public void setHistoryId(Integer id)             { this.historyId = id; }

    public Integer getBuyerId()                      { return buyerId; }
    public void setBuyerId(Integer id)               { this.buyerId = id; }

    public Integer getSellerId()                     { return sellerId; }
    public void setSellerId(Integer id)              { this.sellerId = id; }

    public String getStatus()                        { return status; }
    public void setStatus(String status)             { this.status = status; }

    public String getTitle()                         { return title; }
    public void setTitle(String title)               { this.title = title; }

    public int getFinalPrice()                       { return finalPrice; }
    public void setFinalPrice(int p)                 { this.finalPrice = p; }

    public String getImageUrl()                      { return imageUrl; }
    public void setImageUrl(String url)              { this.imageUrl = url; }

    public Integer getProductId()                    { return productId; }
    public void setProductId(Integer id)             { this.productId = id; }

    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime t)        { this.createdAt = t; }
}
