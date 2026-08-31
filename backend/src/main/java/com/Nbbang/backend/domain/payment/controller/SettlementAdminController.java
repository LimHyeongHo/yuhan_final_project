package com.Nbbang.backend.domain.payment.controller;

import com.Nbbang.backend.domain.payment.dto.AdminWithdrawalResponse;
import com.Nbbang.backend.domain.payment.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// 출금 승인 관리 API. 경로가 /api/admin/** 이라 WebMvcConfig에 이미 등록된 AdminAuthInterceptor가
// 그대로 적용됨 (세션 role=ROLE_ADMIN 아니면 403) - AdminController/AdminService는 건드리지 않음.
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
