package com.Nbbang.backend.domain.admin.security.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SimulationResponse(
        String simulationId,
        String mode,
        Long productId,
        String productTitle,
        BigDecimal originalPrice,
        BigDecimal tamperedPrice,
        BigDecimal currentPrice,
        String originalDbHash,
        String tamperedDbHash,
        String blockchainHash,
        String currentDbHash,
        Integer priceVersion,
        String status,
        String message,
        LocalDateTime startedAt,
        LocalDateTime restoredAt,
        LocalDateTime finishedAt) {
}
