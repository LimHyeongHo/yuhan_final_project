package com.demo.gp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "PRODUCT")
public class Product {

    @Id
    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "seller_id")
    private Integer sellerId;

    private String title;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "base_price")
    private int basePrice;

    @Column(name = "target_qty")
    private int targetQty;   // 최소 참여 인원 기준

    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Integer getProductId()                    { return productId; }
    public void setProductId(Integer id)             { this.productId = id; }

    public Integer getSellerId()                     { return sellerId; }
    public void setSellerId(Integer id)              { this.sellerId = id; }

    public String getTitle()                         { return title; }
    public void setTitle(String title)               { this.title = title; }

    public String getImageUrl()                      { return imageUrl; }
    public void setImageUrl(String url)              { this.imageUrl = url; }

    public int getBasePrice()                        { return basePrice; }
    public void setBasePrice(int p)                  { this.basePrice = p; }

    public int getTargetQty()                        { return targetQty; }
    public void setTargetQty(int qty)                { this.targetQty = qty; }

    public String getContent()                       { return content; }
    public void setContent(String content)           { this.content = content; }

    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime t)        { this.createdAt = t; }
}
