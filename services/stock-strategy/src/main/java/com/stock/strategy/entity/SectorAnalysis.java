package com.stock.strategy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "TB_SECTOR_ANALYSIS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectorAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sector_name", nullable = false)
    private String sectorName;

    @Column(name = "analysis_date", nullable = false)
    private LocalDate analysisDate;

    @Column(name = "avg_momentum_12m", precision = 10, scale = 4)
    private BigDecimal avgMomentum12m;

    @Column(name = "relative_strength", precision = 10, scale = 4)
    private BigDecimal relativeStrength;
}
