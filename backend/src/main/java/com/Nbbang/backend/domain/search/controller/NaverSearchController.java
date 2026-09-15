package com.Nbbang.backend.domain.search.controller;

import com.Nbbang.backend.domain.search.service.BookSearchException;
import com.Nbbang.backend.domain.search.service.GoogleBooksService;
import com.Nbbang.backend.domain.search.service.KakaoBookSearchService;
/// [-] 네이버 검색 api를 사용할 수 없기 때문에 삭제 고려
import com.Nbbang.backend.domain.search.service.NaverSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class NaverSearchController {

    /// [-] 네이버 검색 api 사용 불가능으로 인해 삭제 고려중
    private final NaverSearchService naverSearchService;
    private final KakaoBookSearchService kakaoBookSearchService;
    private final GoogleBooksService googleBooksService;

    /// [*] 에러 검출 및 응답 반환 가능하게 변경
    @GetMapping("/product")
    public ResponseEntity<?> searchProduct(@RequestParam String query, @RequestParam String type) {
        if ("BOOK".equals(type)) {
            try {
                return ResponseEntity.ok(kakaoBookSearchService.searchBook(query));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of(
                        "code", "INVALID_SEARCH_QUERY",
                        "error", e.getMessage()
                ));
            } catch (BookSearchException e) {
                return ResponseEntity.status(e.getStatus()).body(Map.of(
                        "code", e.getCode(),
                        "error", e.getMessage()
                ));
            }
        }

        List<Map<String, String>> result = naverSearchService.search(query);
        if (!result.isEmpty() && result.get(0).containsKey("error")) {
            return ResponseEntity.badRequest().body(result);
        }
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/book-metadata")
    public ResponseEntity<?> getBookMetadata(@RequestParam String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "INVALID_ISBN",
                    "error", "ISBN을 입력해 주세요."
            ));
        }

        GoogleBooksService.BookMetadata metadata = googleBooksService.findByIsbn(isbn);
        return ResponseEntity.ok(Map.of(
                "image", metadata.imageUrl(),
                "category", metadata.category()
        ));
    }
}
