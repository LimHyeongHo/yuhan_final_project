package com.example.aisearchtrend.controller;

import com.example.aisearchtrend.dto.SearchLog;
import com.example.aisearchtrend.dto.TrendResponse;
import com.example.aisearchtrend.service.SearchTrendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchTrendController {

    private final SearchTrendService searchTrendService;

    // 검색어 저장 API
    // 사용자가 도서 검색할 때마다 호출
    @PostMapping("/log")
    public ResponseEntity<?> logSearch(@RequestParam String keyword) {
        try {
            searchTrendService.saveSearch(keyword);
            return ResponseEntity.ok("검색어 저장 완료: " + keyword);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("저장 실패: " + e.getMessage());
        }
    }

    // 검색 횟수 상위 10개 조회
    @GetMapping("/top")
    public ResponseEntity<?> getTopSearches() {
        try {
            List<SearchLog> topSearches = searchTrendService.getTopSearches();
            return ResponseEntity.ok(topSearches);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("조회 실패: " + e.getMessage());
        }
    }

    // ChatGPT 트렌드 분석 API
    @GetMapping("/trend")
    public ResponseEntity<?> getTrend() {
        try {
            TrendResponse trend = searchTrendService.analyzeTrend();
            return ResponseEntity.ok(trend);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("분석 실패: " + e.getMessage());
        }
    }

    // 텍스트 형식으로 보기 좋게 출력
    @GetMapping(value = "/trend/view", produces = "text/plain;charset=UTF-8")
    public ResponseEntity<String> getTrendView() {
        try {
            TrendResponse trend = searchTrendService.analyzeTrend();

            StringBuilder sb = new StringBuilder();
            sb.append("=================================\n");
            sb.append("       📚 도서 트렌드 분석        \n");
            sb.append("=================================\n\n");

            sb.append("🔥 인기 급상승 검색어\n");
            sb.append("---------------------------------\n");
            for (int i = 0; i < trend.getTrendingBooks().size(); i++) {
                sb.append((i + 1) + "위: " + trend.getTrendingBooks().get(i) + "\n");
            }

            sb.append("\n📖 AI 추천 도서\n");
            sb.append("---------------------------------\n");
            for (String book : trend.getRecommendedBooks()) {
                sb.append("• " + book + "\n");
            }

            sb.append("\n💡 트렌드 요약\n");
            sb.append("---------------------------------\n");
            sb.append(trend.getAnalysis() + "\n");

            sb.append("\n생성 시간: " + trend.getGeneratedAt());

            return ResponseEntity.ok(sb.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("분석 실패: " + e.getMessage());
        }
    }

    // 전체 검색 현황 조회
    @GetMapping("/all")
    public ResponseEntity<?> getAllSearchCounts() {
        try {
            Map<String, Integer> counts = searchTrendService.getAllSearchCounts();
            return ResponseEntity.ok(counts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("조회 실패: " + e.getMessage());
        }
    }
}