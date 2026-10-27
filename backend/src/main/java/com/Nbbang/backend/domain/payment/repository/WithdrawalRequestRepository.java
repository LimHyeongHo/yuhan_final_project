package com.Nbbang.backend.domain.payment.repository;

import com.Nbbang.backend.domain.payment.entity.WithdrawalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {
    List<WithdrawalRequest> findBySellerEmailOrderByRequestedAtDesc(String sellerEmail);

    List<WithdrawalRequest> findBySellerEmailAndStatusIn(String sellerEmail, List<String> statuses);

    List<WithdrawalRequest> findByStatusOrderByRequestedAtAsc(String status);
}
