package com.Nbbang.backend.domain.report.repository;

import com.Nbbang.backend.domain.report.entity.UserReportCount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserReportCountRepository extends JpaRepository<UserReportCount, String> {

    Optional<UserReportCount> findByMember_Email(String email);
}
