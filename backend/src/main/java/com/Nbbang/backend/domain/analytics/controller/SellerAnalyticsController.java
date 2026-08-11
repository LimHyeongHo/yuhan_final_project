package com.Nbbang.backend.domain.analytics.controller;

import com.Nbbang.backend.domain.analytics.dto.SellerAnalyticsResponseDto;
import com.Nbbang.backend.domain.analytics.service.SellerAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seller/{id}/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class SellerAnalyticsController {

    private final SellerAnalyticsService sellerAnalyticsService;

    @GetMapping
    public SellerAnalyticsResponseDto getAnalytics(@PathVariable Long id) {
        return sellerAnalyticsService.getSellerAnalytics(id);
    }
}
