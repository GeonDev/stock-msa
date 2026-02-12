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
@Schema(description = "기업 재무 지표 DTO")
public class CorpFinanceIndicatorDto {
    @Schema(description = "기업 코드", example = "00126380")
    private String corpCode;

    @Schema(description = "기준 일자", example = "2024-02-12")
    private LocalDate basDt;

    @Schema(description = "주가수익비율 (PER)", example = "10.5")
    private BigDecimal per;

    @Schema(description = "주가순자산비율 (PBR)", example = "1.2")
    private BigDecimal pbr;

    @Schema(description = "주가매출비율 (PSR)", example = "0.8")
    private BigDecimal psr;

    @Schema(description = "매출액 증가율", example = "5.5")
    private BigDecimal revenueGrowth;

    @Schema(description = "순이익 증가율", example = "10.2")
    private BigDecimal netIncomeGrowth;

    @Schema(description = "영업이익 증가율", example = "8.4")
    private BigDecimal opIncomeGrowth;

    @Schema(description = "자기자본이익률 (ROE)", example = "15.0")
    private BigDecimal roe;

    @Schema(description = "총자산이익률 (ROA)", example = "7.5")
    private BigDecimal roa;

    @Schema(description = "부채비율", example = "45.0")
    private BigDecimal debtRatio;
}
