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
@Table(name = "TB_STOCK_PRICE")
public class StockPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long Id;

    // 단축코드
    @Column(name = "stock_code")
    String stockCode;

    //주식시장 구분
    @Column(name = "market_code")
    String marketCode;

    //기준일
    @Column(name = "bas_dt")
    LocalDate basDt;

    //체결수량의 누적 합계
    @Column(name = "volume")
    Long volume;

    //거래건 별 체결가격 * 체결수량의 누적 합계
    @Column(name = "volume_price")
    Long volumePrice;

    //시초가
    @Column(name = "start_price")
    Long startPrice;

    //종가
    @Column(name = "end_price")
    Long endPrice;

    //일간 최고가
    @Column(name = "high_price")
    Long highPrice;

    //일간 최저가
    @Column(name = "low_price")
    Long lowPrice;

    //전일 대비 등락값
    @Column(name = "daily_range")
    Double dailyRange;

    //전일 대비등락율
    @Column(name = "daily_ratio")
    Double dailyRatio;

    //상장 주식수
    @Column(name = "stock_total_cnt")
    Long stockTotalCnt;

    // 종가 * 상장 주식수 (시가총액)
    @Column(name = "market_total_amt")
    Long marketTotalAmt;

    @OneToOne(mappedBy = "stockPrice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private StockIndicator stockIndicator;
}
