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

    @Schema(description = "부채비율")
    private BigDecimal debtRatio;

    @Schema(description = "매출액 증가율")
    private BigDecimal revenueGrowth;

    @Schema(description = "순이익 증가율")
    private BigDecimal netIncomeGrowth;

    @Schema(description = "영업이익 증가율")
    private BigDecimal opIncomeGrowth;
}
