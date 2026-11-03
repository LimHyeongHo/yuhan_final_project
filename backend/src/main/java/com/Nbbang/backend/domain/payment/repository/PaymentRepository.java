package com.Nbbang.backend.domain.payment.repository;

import com.Nbbang.backend.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(String orderId);

    // 리뷰 작성 자격 확인용
    boolean existsByProductIdAndMember_EmailAndStatus(Long productId, String memberEmail, String status);

    // 출금 가능액 계산용
    List<Payment> findByProductIdInAndStatus(List<Long> productIds, String status);

    Optional<Payment> findFirstByProductIdAndMember_EmailOrderByIdDesc(Long productId, String email);

    Optional<Payment> findFirstByProductIdAndMember_EmailAndStatusInOrderByIdDesc(Long productId, String email,
                                                                                 List<String> statuses);

    // 가격 변경 제한 확인용
    boolean existsByProductIdAndStatus(Long productId, String status);
    
    // 회원 탈퇴 시 buyerName 익명화
    List<Payment> findByMember_Email(String email);

    boolean existsByProductIdAndMember_EmailAndStatusInAndIdNot(Long productId, String email,
                                                               List<String> statuses, Long excludeId);

    // 상품이 삭제됐거나 종료됐는데 환불 안 된 결제 (자동 환불 대상)
    @Query("SELECT p FROM Payment p WHERE p.status IN :statuses AND ("
            + " NOT EXISTS (SELECT 1 FROM Product pr WHERE pr.productId = p.productId)"
            + " OR EXISTS (SELECT 1 FROM Product pr WHERE pr.productId = p.productId AND pr.status IN :productStatuses))")
    List<Payment> findOrphanedPayments(@Param("statuses") List<String> statuses,
                                       @Param("productStatuses") List<String> productStatuses);
}
