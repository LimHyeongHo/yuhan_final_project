package com.Nbbang.backend.domain.product.repository;

import com.Nbbang.backend.domain.product.entity.Scrap;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {
    
    // 사용자의 스크랩 목록 최신순 조회
    List<Scrap> findByMember_EmailOrderByCreatedAtDesc(String email);

    // 단일 상품에 대한 스크랩 여부
    boolean existsByProduct_ProductIdAndMember_Email(Long productId, String email);

    // 스크랩 엔티티 조회 (삭제용)
    Optional<Scrap> findByProduct_ProductIdAndMember_Email(Long productId, String email);
}
