package com.Nbbang.backend.domain.review.repository;

import com.Nbbang.backend.domain.review.entity.Review;
import com.Nbbang.backend.domain.review.entity.Sentiment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 중복 작성 여부 확인
    boolean existsByProductIdAndReviewerEmail(Long productId, String reviewerEmail);

    // 본인 후기 단건 조회
    Optional<Review> findByProductIdAndReviewerEmail(Long productId, String reviewerEmail);

    // 판매자 프로필용 — 해당 판매자에게 달린 후기 최신순
    List<Review> findBySellerEmailOrderByCreatedAtDesc(String sellerEmail);

    // 만족도 집계용 — 감정별 개수
    long countBySellerEmailAndSentiment(String sellerEmail, Sentiment sentiment);
}
