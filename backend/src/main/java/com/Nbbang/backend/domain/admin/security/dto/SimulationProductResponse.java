package com.Nbbang.backend.domain.admin.security.dto;

import java.math.BigDecimal;

public record SimulationProductResponse(
        Long productId,
        String title,
        String category,
        String imageUrl,
        BigDecimal price,
        String productStatus,
        String blockchainStatus,
        Integer currentCount,
        boolean eligible,
        String unavailableReason) {
}
