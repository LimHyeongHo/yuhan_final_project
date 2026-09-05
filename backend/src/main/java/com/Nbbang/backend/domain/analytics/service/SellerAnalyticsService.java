package com.Nbbang.backend.domain.analytics.service;

import com.Nbbang.backend.domain.analytics.dto.SellerAnalyticsResponseDto;
import com.Nbbang.backend.domain.product.entity.Product;
import com.Nbbang.backend.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SellerAnalyticsService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public SellerAnalyticsResponseDto getSellerAnalytics(Long sellerId) {
        // 실제 로그인한 sellerId 로 필터링하여 자신의 판매 데이터만 가져옴
        List<Product> allProducts = productRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);

        return buildAnalytics(allProducts);
    }

    @Transactional(readOnly = true)
    public SellerAnalyticsResponseDto getSellerAnalyticsByEmail(String sellerEmail) {
        return buildAnalytics(productRepository.findBySellerEmailOrderByCreatedAtDesc(sellerEmail));
    }

    private SellerAnalyticsResponseDto buildAnalytics(List<Product> allProducts) {

        long totalRevenue = allProducts.stream()
                .mapToLong(p -> p.getPrice().longValue() * p.getCurrentCount())
                .sum();

        // Top 3 Products
        List<SellerAnalyticsResponseDto.TopProductDto> topProducts = allProducts.stream()
                .sorted((a, b) -> Long.compare(b.getPrice().longValue() * b.getCurrentCount(), a.getPrice().longValue() * a.getCurrentCount()))
                .limit(3)
                .map(p -> SellerAnalyticsResponseDto.TopProductDto.builder()
                        .id(p.getProductId().toString())
                        .title(p.getTitle())
                        .views(p.getCurrentCount() * 105L) // 가상 조회수
                        .sales(p.getCurrentCount())
                        .conversion(String.format("%.2f%%", (p.getCurrentCount() / (double)(p.getCurrentCount() * 105L + 1)) * 100))
                        .build())
                .collect(Collectors.toList());

        // 가상 차트 데이터 생성 (실제로는 날짜별 쿼리를 통해 집계해야 함)
        Map<String, SellerAnalyticsResponseDto.TimeRangeData> filterData = new HashMap<>();
        
        filterData.put("7D", SellerAnalyticsResponseDto.TimeRangeData.builder()
                .label("최근 7일")
                .summary(SellerAnalyticsResponseDto.SummaryData.builder()
                        .views(formatNumber(totalRevenue / 500))
                        .revenue(formatNumber(totalRevenue / 4))
                        .build())
                .chart(List.of(
                        SellerAnalyticsResponseDto.ChartData.builder().day("월").revenue(totalRevenue / 20).build(),
                        SellerAnalyticsResponseDto.ChartData.builder().day("화").revenue(totalRevenue / 15).build(),
                        SellerAnalyticsResponseDto.ChartData.builder().day("수").revenue(totalRevenue / 25).build(),
                        SellerAnalyticsResponseDto.ChartData.builder().day("목").revenue(totalRevenue / 10).build(),
                        SellerAnalyticsResponseDto.ChartData.builder().day("금").revenue(totalRevenue / 8).build(),
                        SellerAnalyticsResponseDto.ChartData.builder().day("토").revenue(totalRevenue / 12).build(),
                        SellerAnalyticsResponseDto.ChartData.builder().day("일").revenue(totalRevenue / 30).build()
                ))
                .build());

        filterData.put("1M", SellerAnalyticsResponseDto.TimeRangeData.builder()
                .label("최근 1개월")
                .summary(SellerAnalyticsResponseDto.SummaryData.builder()
                        .views(formatNumber(totalRevenue / 150))
                        .revenue(formatNumber(totalRevenue / 2))
                        .build())
                .chart(List.of(
                        SellerAnalyticsResponseDto.ChartData.builder().day("1주차").revenue(totalRevenue / 8).build(),
                        SellerAnalyticsResponseDto.ChartData.builder().day("2주차").revenue(totalRevenue / 6).build(),
                        SellerAnalyticsResponseDto.ChartData.builder().day("3주차").revenue(totalRevenue / 7).build(),
                        SellerAnalyticsResponseDto.ChartData.builder().day("4주차").revenue(totalRevenue / 5).build()
                ))
                .build());

        filterData.put("3M", SellerAnalyticsResponseDto.TimeRangeData.builder()
                .label("최근 3개월")
                .summary(SellerAnalyticsResponseDto.SummaryData.builder()
                        .views(formatNumber(totalRevenue / 50))
                        .revenue(formatNumber(totalRevenue))
                        .build())
                .chart(List.of(
                        SellerAnalyticsResponseDto.ChartData.builder().day("4월").revenue(totalRevenue / 4).build(),
                        SellerAnalyticsResponseDto.ChartData.builder().day("5월").revenue(totalRevenue / 3).build(),
                        SellerAnalyticsResponseDto.ChartData.builder().day("6월").revenue(totalRevenue / 2).build()
                ))
                .build());

        return SellerAnalyticsResponseDto.builder()
                .totalCumulativeRevenue(formatNumber(totalRevenue))
                .filterData(filterData)
                .topProducts(topProducts)
                .build();
    }

    private String formatNumber(long number) {
        return NumberFormat.getInstance(Locale.US).format(number);
    }
}
