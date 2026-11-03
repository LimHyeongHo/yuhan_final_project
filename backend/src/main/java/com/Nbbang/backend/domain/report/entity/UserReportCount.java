package com.Nbbang.backend.domain.report.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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

    @Column(nullable = false)
    private int postReportCount = 0;

    @Column(nullable = false)
    private int chatReportCount = 0;
}
