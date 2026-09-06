package com.Nbbang.backend.domain.payment.repository;

import com.Nbbang.backend.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(String orderId);
    // 기본 크루드(저장, 조회, 수정, 삭제) 메서드가 자동으로 제공됩니다.

    // 거래 후기 작성 자격 검증용: 특정 공동구매(productId)에 대한 본인(memberEmail) 결제가 특정 상태(status)로 존재하는지
    boolean existsByProductIdAndMember_EmailAndStatus(Long productId, String memberEmail, String status);

    // 정산(출금 가능액 계산)용: 특정 상품들(productId) 중 특정 상태(status)로 완료된 결제건 조회
    List<Payment> findByProductIdInAndStatus(List<Long> productIds, String status);

    // [신규] PAY-RQ-001: 취소 오케스트레이션에서 본인의 가장 최근 결제건을 상태 무관하게 조회
    // (CANCEL_REQUESTED/REFUND_FAILED 상태에서의 재시도까지 같은 쿼리로 커버하기 위해 상태로 좁히지 않음)
    Optional<Payment> findFirstByProductIdAndMember_EmailOrderByIdDesc(Long productId, String email);

    // [신규] PRD-RQ-004: 가격 변경 제한 판단용 — 이 상품에 결제 완료(DONE) 건이 하나라도 있는지
    boolean existsByProductIdAndStatus(Long productId, String status);
}
