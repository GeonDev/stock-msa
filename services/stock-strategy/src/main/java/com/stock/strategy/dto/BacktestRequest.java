package com.stock.strategy.dto;

import com.stock.common.dto.ValueStrategyConfig;
import com.stock.strategy.enums.RebalancingPeriod;
import com.stock.strategy.enums.StrategyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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
    @NotNull(message = "투자 전략 유형은 필수입니다")
    @Schema(description = "투자 전략 유형", example = "VALUE")
    private StrategyType strategyType;

    @NotNull(message = "백테스팅 시작 날짜는 필수입니다")
    @PastOrPresent(message = "시작 날짜는 현재 또는 과거여야 합니다")
    @Schema(description = "백테스팅 시작 날짜", example = "2023-01-01")
    private LocalDate startDate;

    @NotNull(message = "백테스팅 종료 날짜는 필수입니다")
    @PastOrPresent(message = "종료 날짜는 현재 또는 과거여야 합니다")
    @Schema(description = "백테스팅 종료 날짜", example = "2023-12-31")
    private LocalDate endDate;

    @NotNull(message = "초기 투자 자본금은 필수입니다")
    @DecimalMin(value = "0.01", message = "초기 자본금은 0보다 커야 합니다")
    @Schema(description = "초기 투자 자본금", example = "10000000")
    private BigDecimal initialCapital;

    @NotNull(message = "리밸런싱 주기는 필수입니다")
    @Schema(description = "리밸런싱 주기", example = "MONTHLY")
    private RebalancingPeriod rebalancingPeriod;

    @NotNull(message = "거래 수수료율은 필수입니다")
    @DecimalMin(value = "0.0", inclusive = true, message = "수수료율은 0 이상이어야 합니다")
    @DecimalMax(value = "1.0", inclusive = true, message = "수수료율은 1 이하여야 합니다")
    @Schema(description = "거래 수수료율 (예: 0.0015 = 0.15%)", example = "0.0015")
    private BigDecimal tradingFeeRate;

    @NotNull(message = "거래세율은 필수입니다")
    @DecimalMin(value = "0.0", inclusive = true, message = "거래세율은 0 이상이어야 합니다")
    @DecimalMax(value = "1.0", inclusive = true, message = "거래세율은 1 이하여야 합니다")
    @Schema(description = "거래세율 (예: 0.002 = 0.2%)", example = "0.002")
    private BigDecimal taxRate;

    @Valid
    @Schema(description = "유니버스 필터링 조건")
    private UniverseFilterCriteria universeFilter;

    @Valid
    @Schema(description = "가치 투자 전략 설정 (VALUE 전략 사용 시)")
    private ValueStrategyConfig valueStrategyConfig;
}