package com.stock.strategy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "상세 지표 필터링 조건")
public class CustomFilterCriteria {
    @Schema(description = "최소 PER", example = "0")
    private BigDecimal minPer;

    @Schema(description = "최대 PER", example = "20")
    private BigDecimal maxPer;

    @Schema(description = "최소 PBR", example = "0")
    private BigDecimal minPbr;

    @Schema(description = "최대 PBR", example = "2")
    private BigDecimal maxPbr;

    @Schema(description = "최소 ROE (%)", example = "10")
    private BigDecimal minRoe;

    @Schema(description = "최소 PSR", example = "0")
    private BigDecimal minPsr;

    @Schema(description = "최대 부채비율 (%)", example = "200")
    private BigDecimal maxDebtRatio;

    @Schema(description = "최소 1개월 모멘텀 (%)", example = "5.0")
    private BigDecimal minMomentum1m;

    @Schema(description = "최소 3개월 모멘텀 (%)", example = "10.0")
    private BigDecimal minMomentum3m;

    @Schema(description = "최소 6개월 모멘텀 (%)", example = "20.0")
    private BigDecimal minMomentum6m;

    @Schema(description = "흑자 기업 여부 (true: 흑자 기업만, false: 상관없음)", example = "true")
    private Boolean onlyProfitable;

    @Schema(description = "최소 RSI (14일)", example = "30.0")
    private BigDecimal minRsi14;

    @Schema(description = "최대 RSI (14일)", example = "70.0")
    private BigDecimal maxRsi14;

    @Schema(description = "최소 MACD", example = "0.0")
    private BigDecimal minMacd;

    @Schema(description = "최소 MACD 시그널", example = "0.0")
    private BigDecimal minMacdSignal;

    @Schema(description = "종가가 20일 이평선 위에 있는지 여부", example = "true")
    private Boolean priceAboveMa20;

    @Schema(description = "종가가 60일 이평선 위에 있는지 여부", example = "true")
    private Boolean priceAboveMa60;

    @Schema(description = "종가가 120일 이평선 위에 있는지 여부", example = "true")
    private Boolean priceAboveMa120;
}
