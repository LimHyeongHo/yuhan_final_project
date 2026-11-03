package com.Nbbang.backend.domain.payment.repository;

import com.Nbbang.backend.domain.payment.entity.SettlementAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SettlementAccountRepository extends JpaRepository<SettlementAccount, Long> {
    Optional<SettlementAccount> findBySellerEmail(String sellerEmail);

    // 출금 신청 시 계좌 락 (중복 신청 방지)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM SettlementAccount a WHERE a.sellerEmail = :sellerEmail")
    Optional<SettlementAccount> findBySellerEmailForUpdate(@Param("sellerEmail") String sellerEmail);
}
