package com.Nbbang.backend.domain.report.entity;

import com.Nbbang.backend.domain.auth.entity.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_report_counts")
@Getter
@Setter
public class UserReportCount {

    @Id
    @Column(name = "member_email", length = 100)
    private String memberEmail;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "member_email",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_report_counts_member"))
    private UserAccount member;

    @Column(nullable = false)
    private int postReportCount = 0;

    @Column(nullable = false)
    private int chatReportCount = 0;
}
