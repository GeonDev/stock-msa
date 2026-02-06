package com.stock.strategy.entity;

import com.stock.strategy.enums.RebalancingPeriod;
import com.stock.strategy.enums.SimulationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_BACKTEST_SIMULATION")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestSimulation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "strategy_name", nullable = false, length = 100)
    private String strategyName;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "initial_capital", nullable = false, precision = 19, scale = 2)
    private BigDecimal initialCapital;

    @Enumerated(EnumType.STRING)
    @Column(name = "rebalancing_period", nullable = false, length = 20)
    private RebalancingPeriod rebalancingPeriod;

    @Column(name = "trading_fee_rate", nullable = false, precision = 10, scale = 6)
    private BigDecimal tradingFeeRate;

    @Column(name = "tax_rate", nullable = false, precision = 10, scale = 6)
    private BigDecimal taxRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SimulationStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = SimulationStatus.PENDING;
        }
    }
}
