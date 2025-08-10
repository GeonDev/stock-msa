package com.stock.batch.entity;

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

    String stockCode;

    //주식시장 구분
    String marketCode;

    //기준일
    LocalDate basDt;

    //채결수량
    Integer volume;

    //시초가
    Integer startPrice;

    //종가
    Integer endPrice;

    //일간 최고가
    Integer highPrice;

    //일간 최저가
    Integer lowPrice;

    //전일 대비 등락
    Double dailyRange;

    //등락율
    Double dailyRatio;

    //상장 주식수
    Long stockTotalCnt;

    // 종가 * 상장 주식수 (시가총액)
    Long marketTotalAmt;

}
