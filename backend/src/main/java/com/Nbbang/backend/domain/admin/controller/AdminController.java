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
}
