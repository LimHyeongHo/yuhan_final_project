package com.Nbbang.backend.domain.admin.security.dto;

import java.math.BigDecimal;

public record SimulationStartRequest(
        String mode,
        Long productId,
        BigDecimal newPrice,
        String reason,
        String idempotencyKey) {
}
