package com.Nbbang.backend.domain.payment.controller;

import com.Nbbang.backend.domain.payment.dto.AdminWithdrawalResponse;
import com.Nbbang.backend.domain.payment.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// 관리자 출금 승인/거절 API (AdminAuthInterceptor 적용) → SettlementService
@RestController
@RequestMapping("/api/admin/settlements")
@RequiredArgsConstructor
public class SettlementAdminController {

    private final SettlementService settlementService;

    @GetMapping
    public ResponseEntity<List<AdminWithdrawalResponse>> getPendingWithdrawals() {
        return ResponseEntity.ok(settlementService.getPendingWithdrawals());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long id) {
        settlementService.approveWithdrawal(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        settlementService.rejectWithdrawal(id, reason);
        return ResponseEntity.ok().build();
    }
}
