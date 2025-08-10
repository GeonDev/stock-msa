package com.stock.batch.madel;

import lombok.Data;

@Data
public class StockItem {
    private String basDt;           // 기준일자
    private String srtnCd;          // 단축코드
    private String itmsNm;          // 종목명
    private String mrktCtg;         // 시장구분
    private Long clpr;              // 종가
    private Long vs;                // 대비
    private Double fltRt;           // 등락률
    private Long mkp;               // 시가
    private Long hipr;              // 고가
    private Long lopr;              // 저가
    private Long trqu;              // 거래량
    private Long trPrc;             // 거래대금
    private Long lstgStCnt;         // 상장주식수
    private Long mrktTotAmt;        // 시가총액
}