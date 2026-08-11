package com.Nbbang.backend.domain.product.repository;

import com.Nbbang.backend.domain.product.entity.Participation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    List<Participation> findByProduct_SellerIdOrderByJoinDateDesc(Long sellerId);
    // [신규] sellerEmail 기준 조회 — ProductRepository와 동일한 이유
    List<Participation> findByProduct_SellerEmailOrderByJoinDateDesc(String sellerEmail);

    // 구매 취소 시 가장 최근 참여 기록 하나를 삭제하기 위한 용도
    List<Participation> findByProduct_ProductIdOrderByJoinDateDesc(Long productId);

    // 같은 사용자가 같은 공동구매에 중복 참여하는 것을 막기 위한 체크
    boolean existsByProduct_ProductIdAndMember_Email(Long productId, String email);
}
