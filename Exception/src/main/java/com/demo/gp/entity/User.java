package com.demo.gp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "USERS")
public class User {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    private String email;
    private String nickname;
    private String password;
    private String role;  // ADMIN / SELLER / BUYER

    @Column(name = "miss_count")
    private int missCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Integer getUserId()                       { return userId; }
    public void setUserId(Integer userId)            { this.userId = userId; }

    public String getEmail()                         { return email; }
    public void setEmail(String email)               { this.email = email; }

    public String getNickname()                      { return nickname; }
    public void setNickname(String nickname)         { this.nickname = nickname; }

    public String getPassword()                      { return password; }
    public void setPassword(String password)         { this.password = password; }

    public String getRole()                          { return role; }
    public void setRole(String role)                 { this.role = role; }

    public int getMissCount()                        { return missCount; }
    public void setMissCount(int missCount)          { this.missCount = missCount; }

    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime t)        { this.createdAt = t; }
}
