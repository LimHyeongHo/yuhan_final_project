package com.Nbbang.backend.domain.admin.controller;

import com.Nbbang.backend.domain.admin.service.AdminService;
import com.Nbbang.backend.domain.admin.service.LegacyMigrationJobService;
import com.Nbbang.backend.domain.admin.service.LegacyMigrationWorker;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final LegacyMigrationJobService legacyMigrationJobService;
    private final LegacyMigrationWorker legacyMigrationWorker;

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
    public ResponseEntity<String> grantSellerRole(@PathVariable String email, HttpSession session) {
        String adminEmail = (String) session.getAttribute("userId");
        adminService.grantSellerRole(email, adminEmail);
        return ResponseEntity.ok("판매자 권한이 부여되었습니다.");
    }

    // 주간 통계 조회
    @GetMapping("/weekly-stats")
    public ResponseEntity<Map<String, List<Long>>> getWeeklyStats() {
        return ResponseEntity.ok(adminService.getWeeklyStats());
    }

    // 최근 상품 조회
    @GetMapping("/products/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentProducts() {
        return ResponseEntity.ok(adminService.getRecentProducts());
    }

    // 일간 신규 가입자 목록 (필터 지원)
    @GetMapping("/users/stats-list")
    public ResponseEntity<List<Map<String, Object>>> getUsersStatsList(
            @RequestParam(defaultValue = "1") int days) {
        return ResponseEntity.ok(adminService.getUsersStatsList(days));
    }

    // 누적 활성 판매자 목록
    @GetMapping("/sellers/stats-list")
    public ResponseEntity<List<Map<String, Object>>> getSellersStatsList() {
        return ResponseEntity.ok(adminService.getSellersStatsList());
    }

    // [신규] 어드민용 전체 상품 리스트 조회
    @GetMapping("/products")
    public ResponseEntity<List<Map<String, Object>>> getAllProductsForAdmin() {
        return ResponseEntity.ok(adminService.getAllProductsForAdmin());
    }

    // [신규] 어드민 전용 상품 삭제
    @DeleteMapping("/products/{id}")
    public ResponseEntity<String> deleteProductByAdmin(@PathVariable Long id) {
        adminService.deleteProductByAdmin(id);
        return ResponseEntity.ok("상품이 삭제되었습니다.");
    }

    // [신규] 어드민 전용 상품 거절 (사유 포함)
    @PostMapping("/products/{id}/reject")
    public ResponseEntity<String> rejectProductByAdmin(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        String reason = body.get("reason");
        adminService.rejectProductByAdmin(id, reason);
        return ResponseEntity.ok("상품이 성공적으로 거절(삭제)되었으며, 판매자에게 알림이 발송되었습니다.");
    }

    // [신규] 보안 검증 시뮬레이터: 해킹 시뮬레이션
    @PostMapping("/security/simulate-hack")
    public ResponseEntity<Map<String, Object>> simulateHack() {
        Map<String, Object> result = adminService.simulateHack();
        result.put("message", "해킹 시뮬레이션 성공. (가격 조작 완료)");
        return ResponseEntity.ok(result);
    }

    // [신규] 보안 검증 시뮬레이터: 원상 복구
    @PostMapping("/security/restore/{id}")
    public ResponseEntity<String> restoreHack(@PathVariable Long id) {
        adminService.restoreHack(id);
        return ResponseEntity.ok("정상적으로 복구되었습니다.");
    }

    // [신규] 레거시 데이터 블록체인 마이그레이션
    @PostMapping("/security/migrate-legacy")
    public ResponseEntity<Map<String, Object>> migrateLegacyData() {
        LegacyMigrationJobService.StartResult startResult = legacyMigrationJobService.createJob();
        if (startResult.created()) {
            legacyMigrationWorker.run(startResult.jobId());
        }
        return ResponseEntity.accepted().body(startResult.snapshot());
    }

    @GetMapping("/security/migrate-legacy/{jobId}")
    public ResponseEntity<Map<String, Object>> getLegacyMigrationStatus(@PathVariable String jobId) {
        Map<String, Object> job = legacyMigrationJobService.getJob(jobId);
        return job != null ? ResponseEntity.ok(job) : ResponseEntity.notFound().build();
    }
}
