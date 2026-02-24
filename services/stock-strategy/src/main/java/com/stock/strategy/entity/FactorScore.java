package com.stock.strategy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "TB_FACTOR_SCORE")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FactorScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false)
    private String stockCode;

    @Column(name = "score_date", nullable = false)
    private LocalDate scoreDate;

    @Column(name = "value_score", precision = 10, scale = 4)
    private BigDecimal valueScore;

    @Column(name = "momentum_score", precision = 10, scale = 4)
    private BigDecimal momentumScore;

    @Column(name = "quality_score", precision = 10, scale = 4)
    private BigDecimal qualityScore;

    @Column(name = "total_score", precision = 10, scale = 4)
    private BigDecimal totalScore;
}
