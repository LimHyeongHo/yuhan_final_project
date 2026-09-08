package com.Nbbang.backend.domain.product.repository;

import com.Nbbang.backend.domain.product.entity.Product;
import jakarta.persistence.LockModeType;
import com.Nbbang.backend.domain.product.entity.BlockchainJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
    // [신규] sellerEmail 기준 조회 (sellerId는 항상 1로 고정되는 임시값이라 실사용 불가)
    List<Product> findBySellerEmailOrderByCreatedAtDesc(String sellerEmail);
    // 기본 크루드(저장, 조회, 수정, 삭제) 메서드가 자동으로 제공됩니다.

    // [신규] 스케줄러용: 마감일이 지났고 특정 상태인 상품 조회
    List<Product> findByDeadlineBeforeAndStatus(java.time.LocalDateTime time, String status);

    // [신규] 대시보드용 최근 등록된 5개 상품 조회
    List<Product> findTop5ByOrderByCreatedAtDesc();

    // [신규] PRD-RQ-003: 참여/취소/결제 시 정원 카운트 갱신을 위한 비관적 락 조회 (동시 요청 직렬화)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.productId = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
    List<Product> findByBlockchainStatusIn(List<BlockchainJobStatus> statuses);
}
