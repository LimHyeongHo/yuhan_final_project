package com.Nbbang.backend.domain.product.repository;

import com.Nbbang.backend.domain.product.entity.ProductPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductPriceHistoryRepository extends JpaRepository<ProductPriceHistory, Long> {
    List<ProductPriceHistory> findByProductIdOrderByChangedAtDesc(Long productId);
}
