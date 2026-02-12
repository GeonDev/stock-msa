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
@Schema(description = "주가 기술적 지표 DTO")
public class StockIndicatorDto {
    @Schema(description = "주가 ID", example = "1001")
    private Long id;

    @Schema(description = "종목 코드", example = "005930")
    private String stockCode;

    @Schema(description = "5일 이동평균선", example = "72500.5")
    private BigDecimal ma5;
    @Schema(description = "20일 이동평균선", example = "71800.0")
    private BigDecimal ma20;
    @Schema(description = "60일 이동평균선", example = "70500.0")
    private BigDecimal ma60;
    @Schema(description = "120일 이동평균선", example = "69000.0")
    private BigDecimal ma120;
    @Schema(description = "200일 이동평균선", example = "67000.0")
    private BigDecimal ma200;
    @Schema(description = "250일 이동평균선", example = "66000.0")
    private BigDecimal ma250;

    @Schema(description = "1개월 모멘텀 (%)", example = "5.2")
    private BigDecimal momentum1m;
    @Schema(description = "3개월 모멘텀 (%)", example = "10.5")
    private BigDecimal momentum3m;
    @Schema(description = "6개월 모멘텀 (%)", example = "15.8")
    private BigDecimal momentum6m;

    @Schema(description = "RSI (14일)", example = "65.4")
    private BigDecimal rsi14;

    @Schema(description = "볼린저 밴드 상단", example = "75000.0")
    private BigDecimal bollingerUpper;

    @Schema(description = "볼린저 밴드 하단", example = "68000.0")
    private BigDecimal bollingerLower;

    @Schema(description = "MACD", example = "500.5")
    private BigDecimal macd;

    @Schema(description = "MACD 시그널", example = "450.0")
    private BigDecimal macdSignal;
}
