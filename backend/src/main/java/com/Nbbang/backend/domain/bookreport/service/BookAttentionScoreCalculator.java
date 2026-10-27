package com.Nbbang.backend.domain.bookreport.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class BookAttentionScoreCalculator {

    public int calculate(int relevanceScore, LocalDate publishedAt, BigDecimal price,
                         BigDecimal salePrice, boolean informationComplete, LocalDate reportDate) {
        int freshnessScore = freshnessScore(publishedAt, reportDate);
        int discountScore = discountScore(price, salePrice);
        int completenessScore = informationComplete ? 100 : 50;
        double weighted = relevanceScore * 0.60
                + freshnessScore * 0.25
                + discountScore * 0.10
                + completenessScore * 0.05;
        return Math.max(0, Math.min(100, (int) Math.round(weighted)));
    }

    public int discountRate(BigDecimal price, BigDecimal salePrice) {
        if (price == null || salePrice == null || price.signum() <= 0 || salePrice.signum() < 0
                || salePrice.compareTo(price) >= 0) {
            return 0;
        }
        return price.subtract(salePrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(price, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private int freshnessScore(LocalDate publishedAt, LocalDate reportDate) {
        if (publishedAt == null || reportDate == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(publishedAt, reportDate);
        if (days < 0 || days > 365) {
            return 0;
        }
        if (days <= 30) {
            return 100;
        }
        if (days <= 90) {
            return 70;
        }
        return 40;
    }

    private int discountScore(BigDecimal price, BigDecimal salePrice) {
        return Math.min(100, discountRate(price, salePrice) * 5);
    }
}
