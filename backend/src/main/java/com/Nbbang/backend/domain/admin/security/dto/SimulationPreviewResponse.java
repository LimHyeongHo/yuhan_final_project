package com.Nbbang.backend.domain.admin.security.dto;

import java.math.BigDecimal;

public record SimulationPreviewResponse(
        Long productId,
        String title,
        BigDecimal currentPrice,
        String dbHash,
        String blockchainHash,
        String status,
        String message) {
}
