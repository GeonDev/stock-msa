package com.stock.strategy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "TB_BACKTEST_RESULT")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "simulation_id", nullable = false, unique = true)
    private Long simulationId;

    @Column(name = "final_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal finalValue;

    @Column(name = "total_return", precision = 10, scale = 4)
    private BigDecimal totalReturn;

    @Column(name = "cagr", precision = 10, scale = 4)
    private BigDecimal cagr;

    @Column(name = "mdd", precision = 10, scale = 4)
    private BigDecimal mdd;

    @Column(name = "sharpe_ratio", precision = 10, scale = 4)
    private BigDecimal sharpeRatio;

    @Column(name = "volatility", precision = 10, scale = 4)
    private BigDecimal volatility;

    @Column(name = "win_rate", precision = 10, scale = 4)
    private BigDecimal winRate;

    @Column(name = "total_trades")
    private Integer totalTrades;

    @Column(name = "profitable_trades")
    private Integer profitableTrades;

    @Column(name = "is_optimized")
    private Boolean isOptimized;

    @Column(name = "slippage_type", length = 20)
    private String slippageType;
}
