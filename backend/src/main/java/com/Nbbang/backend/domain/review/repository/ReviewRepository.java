package com.Nbbang.backend.domain.review.repository;

import com.Nbbang.backend.domain.review.entity.Review;
import com.Nbbang.backend.domain.review.entity.Sentiment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 이미 쓴 후기가 있는지 확인
    boolean existsByProductIdAndReviewerEmail(Long productId, String reviewerEmail);

    // 내 후기 한 건 조회
    Optional<Review> findByProductIdAndReviewerEmail(Long productId, String reviewerEmail);

    // 판매자 프로필용, 그 판매자가 받은 후기 최신순
    List<Review> findBySellerEmailOrderByCreatedAtDesc(String sellerEmail);

    // 평가별 개수 (관리자 화면 만족도 계산에 사용)
    long countBySellerEmailAndSentiment(String sellerEmail, Sentiment sentiment);
}
