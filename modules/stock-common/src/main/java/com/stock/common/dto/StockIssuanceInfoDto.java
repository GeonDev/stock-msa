package com.stock.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockIssuanceInfoDto {
    private String stockCode;    // 단축코드
    private String eventType;    // 증자/감자 구분
    private String eventDate;    // 유효일자/기준일
    private BigDecimal issuanceRatio; // 배정비율
    private BigDecimal issuanceAmount;  // 발행금액/액면가
    private BigDecimal listedStockCnt;  // 상장주식수
}
