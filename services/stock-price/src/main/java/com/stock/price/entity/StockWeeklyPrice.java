package com.stock.price.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "TB_STOCK_WEEKLY_PRICE")
public class StockWeeklyPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long Id;

    @Column(name = "stock_code")
    String stockCode;

    @Column(name = "market_code")
    String marketCode;

    @Column(name = "start_date")
    LocalDate startDate;

    @Column(name = "end_date")
    LocalDate endDate;

    @Column(name = "volume")
    Long volume;

    @Column(name = "volume_price")
    Long volumePrice;

    @Column(name = "start_price")
    Long startPrice;

    @Column(name = "end_price")
    Long endPrice;

    @Column(name = "high_price")
    Long highPrice;

    @Column(name = "low_price")
    Long lowPrice;

    @Column(name = "stock_total_cnt")
    Long stockTotalCnt;

    @Column(name = "market_total_amt")
    Long marketTotalAmt;
}
