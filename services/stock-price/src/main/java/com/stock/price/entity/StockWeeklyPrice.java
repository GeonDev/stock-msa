package com.stock.price.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "TB_STOCK_WEEKLY_PRICE")
public class StockWeeklyPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code")
    private String stockCode;

    @Column(name = "market_code")
    private String marketCode;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "volume")
    private BigDecimal volume;

    @Column(name = "volume_price")
    private BigDecimal volumePrice;

    @Column(name = "start_price")
    private BigDecimal startPrice;

    @Column(name = "end_price")
    private BigDecimal endPrice;

    @Column(name = "high_price")
    private BigDecimal highPrice;

    @Column(name = "low_price")
    private BigDecimal lowPrice;

    @Column(name = "stock_total_cnt")
    private BigDecimal stockTotalCnt;

    @Column(name = "market_total_amt")
    private BigDecimal marketTotalAmt;
}
