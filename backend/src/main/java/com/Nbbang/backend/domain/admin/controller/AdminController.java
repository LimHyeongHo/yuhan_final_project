package com.Nbbang.backend.domain.admin.controller;

import com.Nbbang.backend.domain.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // 대시보드 통계 조회
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(adminService.getDashboardStatistics());
    }

    // 판매자 승인 대기열 조회
    @GetMapping("/users/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingSellers() {
        return ResponseEntity.ok(adminService.getPendingSellers());
    }

    // 판매자 권한 부여
    @PostMapping("/users/{email}/grant-seller")
    public ResponseEntity<String> grantSellerRole(@PathVariable String email) {
        adminService.grantSellerRole(email);
        return ResponseEntity.ok("판매자 권한이 부여되었습니다.");
    }
}
