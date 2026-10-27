package com.Nbbang.backend.domain.bookreport.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BookAttentionScoreCalculatorTest {

    private final BookAttentionScoreCalculator calculator = new BookAttentionScoreCalculator();

    @Test
    void calculatesTheDocumentedDeterministicScore() {
        int score = calculator.calculate(
                80,
                LocalDate.of(2026, 10, 10),
                new BigDecimal("30000"),
                new BigDecimal("27000"),
                true,
                LocalDate.of(2026, 10, 27)
        );

        assertThat(score).isEqualTo(83);
        assertThat(calculator.discountRate(new BigDecimal("30000"), new BigDecimal("27000")))
                .isEqualTo(10);
    }

    @Test
    void treatsInvalidPriceAndUnknownPublicationDateAsZero() {
        assertThat(calculator.discountRate(BigDecimal.ZERO, BigDecimal.ZERO)).isZero();
        assertThat(calculator.calculate(0, null, null, null, false, LocalDate.of(2026, 10, 27)))
                .isEqualTo(3);
    }
}
