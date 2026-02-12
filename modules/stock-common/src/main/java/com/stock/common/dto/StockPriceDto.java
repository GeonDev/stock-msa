package com.stock.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "주가 정보 DTO")
public class StockPriceDto {
    @Schema(description = "ID", example = "1")
    private Long id;

    @Schema(description = "종목 코드", example = "005930")
    private String stockCode;

    @Schema(description = "시장 코드", example = "KOSPI")
    private String marketCode;

    @Schema(description = "기준 일자", example = "2024-02-12")
    private LocalDate basDt;

    @Schema(description = "거래량", example = "15000000")
    private BigDecimal volume;

    @Schema(description = "거래대금", example = "1100000000000")
    private BigDecimal volumePrice;

    @Schema(description = "시가", example = "73000")
    private BigDecimal startPrice;

    @Schema(description = "종가", example = "72500")
    private BigDecimal endPrice;

    @Schema(description = "고가", example = "74000")
    private BigDecimal highPrice;

    @Schema(description = "저가", example = "72000")
    private BigDecimal lowPrice;

    @Schema(description = "대비 (전일 대비 변동폭)", example = "-500")
    private BigDecimal dailyRange;

    @Schema(description = "등락률", example = "-0.68")
    private BigDecimal dailyRatio;

    @Schema(description = "상장주식수", example = "5969782550")
    private BigDecimal stockTotalCnt;

    @Schema(description = "시가총액", example = "432809234875000")
    private BigDecimal marketTotalAmt;
}
