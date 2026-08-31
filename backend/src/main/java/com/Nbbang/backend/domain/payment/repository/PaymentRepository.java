package com.Nbbang.backend.domain.payment.repository;

import com.Nbbang.backend.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(String orderId);
    // 기본 크루드(저장, 조회, 수정, 삭제) 메서드가 자동으로 제공됩니다.

    // 거래 후기 작성 자격 검증용: 특정 공동구매(productId)에 대한 본인(memberEmail) 결제가 특정 상태(status)로 존재하는지
    boolean existsByProductIdAndMember_EmailAndStatus(Long productId, String memberEmail, String status);
}
