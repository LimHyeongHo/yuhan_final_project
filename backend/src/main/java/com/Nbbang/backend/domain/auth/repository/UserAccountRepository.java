package com.Nbbang.backend.domain.auth.repository;

import com.Nbbang.backend.domain.auth.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, String> {
    
    // [신규] 관리자 권한 조회용
    long countByRole(String role);
    long countByCreatedAtAfter(java.time.LocalDateTime time);
    long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
    long countByRoleAndCreatedAtBetween(String role, java.time.LocalDateTime start, java.time.LocalDateTime end);
    java.util.List<UserAccount> findByRoleOrderByCreatedAtDesc(String role);
}
