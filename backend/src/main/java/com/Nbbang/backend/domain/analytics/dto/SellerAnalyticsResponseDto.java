package com.Nbbang.backend.domain.analytics.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class SellerAnalyticsResponseDto {
    private String totalCumulativeRevenue;
    private Map<String, TimeRangeData> filterData;
    private List<TopProductDto> topProducts;

    @Getter
    @Builder
    public static class TimeRangeData {
        private String label;
        private SummaryData summary;
        private List<ChartData> chart;
    }

    @Getter
    @Builder
    public static class SummaryData {
        private String views;
        private String revenue;
    }

    @Getter
    @Builder
    public static class ChartData {
        private String day;
        private long revenue;
    }

    @Getter
    @Builder
    public static class TopProductDto {
        private String id;
        private String title;
        private long views;
        private long sales;
        private String conversion;
    }
}
