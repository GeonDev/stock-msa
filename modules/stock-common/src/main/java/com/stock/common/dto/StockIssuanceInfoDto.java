package com.stock.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "주식 발행 정보 (증자/감자) DTO")
public class StockIssuanceInfoDto {
    @Schema(description = "종목 코드", example = "005930")
    private String stockCode;    // 단축코드

    @Schema(description = "증자/감자 구분", example = "무상증자")
    private String eventType;    // 증자/감자 구분

    @Schema(description = "유효일자/기준일", example = "20240101")
    private String eventDate;    // 유효일자/기준일

    @Schema(description = "배정비율", example = "0.1")
    private BigDecimal issuanceRatio; // 배정비율

    @Schema(description = "발행금액/액면가", example = "5000")
    private BigDecimal issuanceAmount;  // 발행금액/액면가

    @Schema(description = "상장주식수", example = "5969782550")
    private BigDecimal listedStockCnt;  // 상장주식수
}
