package com.Nbbang.backend.domain.payment.repository;

import com.Nbbang.backend.domain.payment.entity.WithdrawalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {
    List<WithdrawalRequest> findBySellerEmailOrderByRequestedAtDesc(String sellerEmail);

    // 출금 가능액 계산 시 이미 묶여있는(신청중) 또는 지급 완료된 금액을 차감하기 위함
    List<WithdrawalRequest> findBySellerEmailAndStatusIn(String sellerEmail, List<String> statuses);

    // 관리자 승인 대기열
    List<WithdrawalRequest> findByStatusOrderByRequestedAtAsc(String status);
}
