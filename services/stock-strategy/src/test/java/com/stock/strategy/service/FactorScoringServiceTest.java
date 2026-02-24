package com.stock.strategy.service;

import com.stock.common.dto.CorpFinanceIndicatorDto;
import com.stock.common.dto.StockIndicatorDto;
import com.stock.strategy.entity.FactorScore;
import com.stock.strategy.repository.FactorScoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FactorScoringServiceTest {

    @Mock
    private FactorScoreRepository factorScoreRepository;

    @InjectMocks
    private FactorScoringService factorScoringService;

    @Test
    @DisplayName("Z-Score 계산 정확도 검증")
    void calculateAndSaveScoresTest() {
        // given
        LocalDate date = LocalDate.now();
        List<CorpFinanceIndicatorDto> finances = Arrays.asList(
                CorpFinanceIndicatorDto.builder().corpCode("A").per(new BigDecimal("10")).pbr(new BigDecimal("1")).roe(new BigDecimal("10")).build(),
                CorpFinanceIndicatorDto.builder().corpCode("B").per(new BigDecimal("20")).pbr(new BigDecimal("2")).roe(new BigDecimal("20")).build(),
                CorpFinanceIndicatorDto.builder().corpCode("C").per(new BigDecimal("30")).pbr(new BigDecimal("3")).roe(new BigDecimal("30")).build()
        );

        List<StockIndicatorDto> indicators = Arrays.asList(
                StockIndicatorDto.builder().stockCode("A").momentum6m(new BigDecimal("5")).build(),
                StockIndicatorDto.builder().stockCode("B").momentum6m(new BigDecimal("10")).build(),
                StockIndicatorDto.builder().stockCode("C").momentum6m(new BigDecimal("15")).build()
        );

        when(factorScoreRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        // weight: value(0.4), momentum(0.3), quality(0.3)
        List<FactorScore> scores = factorScoringService.calculateAndSaveScores(
                date, finances, indicators,
                new BigDecimal("0.4"), new BigDecimal("0.3"), new BigDecimal("0.3")
        );

        // then
        assertThat(scores).hasSize(3);

        // A is best value (lowest PER/PBR), worst momentum, worst quality
        // C is worst value (highest PER/PBR), best momentum, best quality
        // They should have opposite z-scores.

        FactorScore scoreA = scores.stream().filter(s -> s.getStockCode().equals("A")).findFirst().get();
        FactorScore scoreC = scores.stream().filter(s -> s.getStockCode().equals("C")).findFirst().get();

        // Since A has lowest PER and PBR, its valueZ should be highly positive.
        // Since C has highest PER and PBR, its valueZ should be highly negative.
        assertThat(scoreA.getValueScore()).isGreaterThan(BigDecimal.ZERO);
        assertThat(scoreC.getValueScore()).isLessThan(BigDecimal.ZERO);

        // Since A has lowest momentum and quality, its momentum and quality Z should be negative.
        assertThat(scoreA.getMomentumScore()).isLessThan(BigDecimal.ZERO);
        assertThat(scoreA.getQualityScore()).isLessThan(BigDecimal.ZERO);
    }
}
