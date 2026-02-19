package com.stock.finance.entity;


import com.stock.common.enums.ReportCode;
import com.stock.common.enums.ValidationStatus;
import com.stock.finance.converter.ReportCodeConverter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;


@Data
@Entity
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@IdClass(CorpFinanceId.class)
@Table(name = "TB_CORP_FINANCE")
@Schema(description = "기업 재무 정보")
public class CorpFinance implements Serializable {

    @Id
    @Schema(description = "법인번호", example = "1101110000000")
    @Column(name = "corp_code")
    String corpCode;

    @Id
    @Schema(description = "법인세의 과세기간 (사업연도)", example = "2023")
    @Column(name = "biz_year")
    String bizYear;

    @Id
    @Convert(converter = ReportCodeConverter.class)
    @Schema(description = "보고서 코드", example = "ANNUAL")
    @Column(name = "report_code", columnDefinition = "VARCHAR(5)")
    ReportCode reportCode;

    @Schema(description = "거래 기준 일자", example = "2024-01-01")
    @Column(name = "bas_dt")
    LocalDate basDt;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status")
    private ValidationStatus validationStatus;

    @Schema(description = "통화 코드", example = "KRW")
    @Column(name = "currency")
    String currency;

    @Schema(description = "영업이익")
    @Column(name = "op_income")
    BigDecimal opIncome;

    @Schema(description = "순이익")
    @Column(name = "net_income")
    BigDecimal netIncome;

    @Schema(description = "매출액")
    @Column(name = "revenue")
    BigDecimal revenue;

    @Schema(description = "기업 총 자산")
    @Column(name = "total_asset")
    BigDecimal totalAsset;

    @Schema(description = "기업 총 부채")
    @Column(name = "total_debt")
    BigDecimal totalDebt;

    @Schema(description = "총 자본")
    @Column(name = "total_capital")
    BigDecimal totalCapital;

    @Schema(description = "영업활동 현금흐름")
    @Column(name = "operating_cashflow")
    BigDecimal operatingCashflow;

    @Schema(description = "투자활동 현금흐름")
    @Column(name = "investing_cashflow")
    BigDecimal investingCashflow;

    @Schema(description = "재무활동 현금흐름")
    @Column(name = "financing_cashflow")
    BigDecimal financingCashflow;

    @Schema(description = "잉여현금흐름 (FCF = 영업CF - 투자CF)")
    @Column(name = "free_cashflow")
    BigDecimal freeCashflow;

    @Schema(description = "EBITDA (법인세/이자/감가상각비 차감 전 영업이익)")
    @Column(name = "ebitda")
    BigDecimal ebitda;

    @Schema(description = "감가상각비")
    @Column(name = "depreciation")
    BigDecimal depreciation;

    @OneToOne(mappedBy = "corpFinance", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Schema(description = "재무 지표 정보")
    private CorpFinanceIndicator corpFinanceIndicator;
}
