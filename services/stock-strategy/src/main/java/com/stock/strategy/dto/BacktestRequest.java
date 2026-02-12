package com.stock.strategy.dto;

import com.stock.strategy.enums.RebalancingPeriod;
import com.stock.strategy.enums.StrategyType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "백테스팅 요청 정보")
public class BacktestRequest {
    @Schema(description = "투자 전략 유형", example = "VALUE")
    private StrategyType strategyType;

    @Schema(description = "백테스팅 시작 날짜", example = "2023-01-01")
    private LocalDate startDate;

    @Schema(description = "백테스팅 종료 날짜", example = "2023-12-31")
    private LocalDate endDate;

    @Schema(description = "초기 투자 자본금", example = "10000000")
    private BigDecimal initialCapital;

    @Schema(description = "리밸런싱 주기", example = "MONTHLY")
    private RebalancingPeriod rebalancingPeriod;

    @Schema(description = "거래 수수료율 (예: 0.0015 = 0.15%)", example = "0.0015")
    private BigDecimal tradingFeeRate;

    @Schema(description = "거래세율 (예: 0.002 = 0.2%)", example = "0.002")
    private BigDecimal taxRate;

    @Schema(description = "유니버스 필터링 조건")
    private UniverseFilterCriteria universeFilter;
}