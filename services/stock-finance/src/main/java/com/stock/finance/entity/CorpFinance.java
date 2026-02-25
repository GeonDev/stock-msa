package com.stock.finance.entity;

import com.stock.common.enums.ReportCode;
import com.stock.common.enums.ValidationStatus;
import com.stock.finance.converter.ReportCodeConverter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "TB_CORP_FINANCE",
        uniqueConstraints = @UniqueConstraint(columnNames = {"corp_code", "biz_year", "report_code"}))
@Schema(description = "기업 재무 정보")
public class CorpFinance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Schema(description = "법인번호", example = "A005930")
    @Column(name = "corp_code", nullable = false)
    private String corpCode;

    @Schema(description = "사업연도", example = "2023")
    @Column(name = "biz_year", nullable = false, length = 4)
    private String bizYear;

    @Schema(description = "보고서 코드", example = "ANNUAL")
    @Convert(converter = ReportCodeConverter.class)
    @Column(name = "report_code", nullable = false, columnDefinition = "VARCHAR(5)")
    private ReportCode reportCode;

    @Schema(description = "기준일자", example = "2024-01-01")
    @Column(name = "bas_dt")
    private LocalDate basDt;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status")
    private ValidationStatus validationStatus;

    @Schema(description = "통화 코드", example = "KRW")
    @Column(name = "currency")
    private String currency;

    @Schema(description = "영업이익")
    @Column(name = "op_income")
    private BigDecimal opIncome;

    @Schema(description = "순이익")
    @Column(name = "net_income")
    private BigDecimal netIncome;

    @Schema(description = "매출액")
    @Column(name = "revenue")
    private BigDecimal revenue;

    @Schema(description = "기업 총 자산")
    @Column(name = "total_asset")
    private BigDecimal totalAsset;

    @Schema(description = "기업 총 부채")
    @Column(name = "total_debt")
    private BigDecimal totalDebt;

    @Schema(description = "총 자본")
    @Column(name = "total_capital")
    private BigDecimal totalCapital;

    @Schema(description = "영업활동 현금흐름")
    @Column(name = "operating_cashflow")
    private BigDecimal operatingCashflow;

    @Schema(description = "투자활동 현금흐름")
    @Column(name = "investing_cashflow")
    private BigDecimal investingCashflow;

    @Schema(description = "재무활동 현금흐름")
    @Column(name = "financing_cashflow")
    private BigDecimal financingCashflow;

    @Schema(description = "잉여현금흐름 (FCF = 영업CF - 투자CF)")
    @Column(name = "free_cashflow")
    private BigDecimal freeCashflow;

    @Schema(description = "EBITDA (법인세/이자/감가상각비 차감 전 영업이익)")
    @Column(name = "ebitda")
    private BigDecimal ebitda;

    @Schema(description = "감가상각비")
    @Column(name = "depreciation")
    private BigDecimal depreciation;

    @OneToOne(mappedBy = "corpFinance", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Schema(description = "재무 지표 정보")
    private CorpFinanceIndicator corpFinanceIndicator;
}
