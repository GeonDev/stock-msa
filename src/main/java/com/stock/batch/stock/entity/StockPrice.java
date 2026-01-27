package com.stock.batch.stock.entity;

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
    String stockCode;

    //주식시장 구분
    String marketCode;

    //기준일
    LocalDate basDt;

    //체결수량의 누적 합계
    Integer volume;

    //거래건 별 체결가격 * 체결수량의 누적 합계
    Long volumePrice;

    //시초가
    Integer startPrice;

    //종가
    Integer endPrice;

    //일간 최고가
    Integer highPrice;

    //일간 최저가
    Integer lowPrice;

    //전일 대비 등락값
    Double dailyRange;

    //전일 대비등락율
    Double dailyRatio;

    //상장 주식수
    Long stockTotalCnt;

    // 종가 * 상장 주식수 (시가총액)
    Long marketTotalAmt;

    @OneToOne(mappedBy = "stockPrice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private StockIndicator stockIndicator;
}
