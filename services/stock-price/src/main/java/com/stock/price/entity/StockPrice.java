package com.stock.price.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "TB_STOCK_PRICE")
public class StockPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 단축코드
    @Column(name = "stock_code")
    private String stockCode;

    //주식시장 구분
    @Column(name = "market_code")
    private String marketCode;

    //기준일
    @Column(name = "bas_dt")
    private LocalDate basDt;

    //수정 종가
    @Column(name = "adj_close_price")
    private BigDecimal adjClosePrice;

    //체결수량의 누적 합계
    @Column(name = "volume")
    private BigDecimal volume;

    //거래건 별 체결가격 * 체결수량의 누적 합계
    @Column(name = "volume_price")
    private BigDecimal volumePrice;

    //시초가
    @Column(name = "start_price")
    private BigDecimal startPrice;

    //종가
    @Column(name = "end_price")
    private BigDecimal endPrice;

    //일간 최고가
    @Column(name = "high_price")
    private BigDecimal highPrice;

    //일간 최저가
    @Column(name = "low_price")
    private BigDecimal lowPrice;

    //전일 대비 등락값
    @Column(name = "daily_range")
    private BigDecimal dailyRange;

    //전일 대비등락율
    @Column(name = "daily_ratio")
    private BigDecimal dailyRatio;

    //상장 주식수
    @Column(name = "stock_total_cnt")
    private BigDecimal stockTotalCnt;

    // 종가 * 상장 주식수 (시가총액)
    @Column(name = "market_total_amt")
    private BigDecimal marketTotalAmt;

    // 시가총액 순위 (시장별)
    @Column(name = "market_cap_rank")
    private Integer marketCapRank;

    // 시가총액 분위수 (시장별, 0-100)
    @Column(name = "market_cap_percentile")
    private BigDecimal marketCapPercentile;

    @JsonIgnore
    @OneToOne(mappedBy = "stockPrice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private StockIndicator stockIndicator;
}
