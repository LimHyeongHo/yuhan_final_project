package com.Nbbang.backend.domain.payment.dto;

import lombok.Data;

@Data
public class PaymentPrepareRequest {
    private Long productId;
    // buyerName은 더 이상 클라이언트에서 받지 않음 - 로그인 세션의 실제 계정에서 가져옴 (PaymentService.prepare 참고)
}
