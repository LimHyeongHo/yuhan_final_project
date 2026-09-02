package com.Nbbang.backend.domain.payment.repository;

import com.Nbbang.backend.domain.payment.entity.SettlementAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SettlementAccountRepository extends JpaRepository<SettlementAccount, Long> {
    Optional<SettlementAccount> findBySellerEmail(String sellerEmail);
}
