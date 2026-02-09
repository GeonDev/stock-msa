package com.stock.strategy.dto;

import com.stock.strategy.enums.RebalancingPeriod;
import com.stock.strategy.enums.StrategyType;
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
public class BacktestRequest {
    private StrategyType strategyType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal initialCapital;
    private RebalancingPeriod rebalancingPeriod;
    private BigDecimal tradingFeeRate;
    private BigDecimal taxRate;
    private UniverseFilterCriteria universeFilter;
}
