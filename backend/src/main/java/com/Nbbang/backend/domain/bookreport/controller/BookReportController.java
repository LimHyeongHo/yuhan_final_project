package com.Nbbang.backend.domain.bookreport.controller;

import com.Nbbang.backend.domain.bookreport.dto.BookReportResponse;
import com.Nbbang.backend.domain.bookreport.service.BookReportQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/book-reports")
@RequiredArgsConstructor
public class BookReportController {

    private final BookReportQueryService queryService;

    @GetMapping("/latest")
    public ResponseEntity<BookReportResponse> getLatest(@RequestParam String category) {
        return ResponseEntity.ok(queryService.getLatest(category));
    }
}
