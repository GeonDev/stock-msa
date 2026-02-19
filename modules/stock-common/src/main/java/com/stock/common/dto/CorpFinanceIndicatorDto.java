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
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "기업 재무 지표 DTO")
public class CorpFinanceIndicatorDto {
    @Schema(description = "기업 코드", example = "005930")
    private String corpCode;

    @Schema(description = "기준 일자")
    private LocalDate basDt;

    @Schema(description = "주가수익비율 (PER)")
    private BigDecimal per;

    @Schema(description = "주가순자산비율 (PBR)")
    private BigDecimal pbr;

    @Schema(description = "주가매출비율 (PSR)")
    private BigDecimal psr;

    @Schema(description = "자기자본이익률 (ROE)")
    private BigDecimal roe;

    @Schema(description = "총자산이익률 (ROA)")
    private BigDecimal roa;

    @Schema(description = "주가현금흐름비율 (PCR)")
    private BigDecimal pcr;

    @Schema(description = "EV/EBITDA")
    private BigDecimal evEbitda;

    @Schema(description = "FCF Yield (%)")
    private BigDecimal fcfYield;

    @Schema(description = "영업이익률 (%)")
    private BigDecimal operatingMargin;

    @Schema(description = "순이익률 (%)")
    private BigDecimal netMargin;

    @Schema(description = "QoQ 매출 성장률 (%)")
    private BigDecimal qoqRevenueGrowth;

    @Schema(description = "QoQ 영업이익 성장률 (%)")
    private BigDecimal qoqOpIncomeGrowth;

    @Schema(description = "QoQ 순이익 성장률 (%)")
    private BigDecimal qoqNetIncomeGrowth;

    @Schema(description = "YoY 매출 성장률 (%)")
    private BigDecimal yoyRevenueGrowth;

    @Schema(description = "YoY 영업이익 성장률 (%)")
    private BigDecimal yoyOpIncomeGrowth;

    @Schema(description = "YoY 순이익 성장률 (%)")
    private BigDecimal yoyNetIncomeGrowth;
}
