package com.stock.finance.entity;

import com.stock.common.enums.ReportCode;
import com.stock.finance.converter.ReportCodeConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@IdClass(CorpFinanceIndicatorId.class)
@Table(name = "TB_CORP_FINANCE_INDICATOR")
public class CorpFinanceIndicator {

    @Id
    @Column(name = "corp_code")
    private String corpCode;

    @Id
    @Column(name = "bas_dt")
    private java.time.LocalDate basDt;

    @Id
    @Convert(converter = ReportCodeConverter.class)
    @Column(name = "report_code", columnDefinition = "VARCHAR(5)")
    private ReportCode reportCode;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumns(value = {
            @JoinColumn(name = "corp_code", referencedColumnName = "corp_code"),
            @JoinColumn(name = "bas_dt", referencedColumnName = "bas_dt"),
            @JoinColumn(name = "report_code", referencedColumnName = "report_code")
    }, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private CorpFinance corpFinance;

    /**
     * 주가수익비율 (Price-to-Earnings Ratio)
     * 주가를 주당순이익(EPS)으로 나눈 값. 낮을수록 저평가.
     */
    @Column(name = "per")
    private BigDecimal per;

    /**
     * 주가순자산비율 (Price-to-Book-Value Ratio)
     * 주가를 주당순자산가치(BPS)로 나눈 값. 낮을수록 저평가.
     */
    @Column(name = "pbr")
    private BigDecimal pbr;

    /**
     * 주가매출비율 (Price-to-Sales Ratio)
     * 주가를 주당매출액(SPS)으로 나눈 값. 성장주 가치 평가에 사용.
     */
    @Column(name = "psr")
    private BigDecimal psr;

    /**
     * 자기자본이익률 (Return on Equity)
     * 순이익을 자기자본으로 나눈 값. 높을수록 수익성이 좋음.
     */
    @Column(name = "roe")
    private BigDecimal roe;

    /**
     * 총자산이익률 (Return on Assets)
     * 순이익을 총자산으로 나눈 값. 높을수록 자산 활용 효율이 좋음.
     */
    @Column(name = "roa")
    private BigDecimal roa;

    /**
     * 주가현금흐름비율 (Price-to-Cashflow Ratio)
     * 시가총액을 영업현금흐름으로 나눈 값. 낮을수록 저평가.
     */
    @Column(name = "pcr")
    private BigDecimal pcr;

    /**
     * EV/EBITDA (Enterprise Value to EBITDA)
     * 기업가치를 EBITDA로 나눈 값. 낮을수록 저평가.
     */
    @Column(name = "ev_ebitda")
    private BigDecimal evEbitda;

    /**
     * FCF Yield (Free Cashflow Yield)
     * 잉여현금흐름을 시가총액으로 나눈 값 (%). 높을수록 좋음.
     */
    @Column(name = "fcf_yield")
    private BigDecimal fcfYield;

    /**
     * 영업이익률 (Operating Margin)
     * 영업이익을 매출액으로 나눈 값 (%). 높을수록 수익성이 좋음.
     */
    @Column(name = "operating_margin")
    private BigDecimal operatingMargin;

    /**
     * 순이익률 (Net Margin)
     * 순이익을 매출액으로 나눈 값 (%). 높을수록 수익성이 좋음.
     */
    @Column(name = "net_margin")
    private BigDecimal netMargin;

    /**
     * QoQ 매출 성장률 (Quarter over Quarter Revenue Growth)
     * 전분기 대비 매출 성장률 (%).
     */
    @Column(name = "qoq_revenue_growth")
    private BigDecimal qoqRevenueGrowth;

    /**
     * QoQ 영업이익 성장률 (Quarter over Quarter Operating Income Growth)
     * 전분기 대비 영업이익 성장률 (%).
     */
    @Column(name = "qoq_op_income_growth")
    private BigDecimal qoqOpIncomeGrowth;

    /**
     * QoQ 순이익 성장률 (Quarter over Quarter Net Income Growth)
     * 전분기 대비 순이익 성장률 (%).
     */
    @Column(name = "qoq_net_income_growth")
    private BigDecimal qoqNetIncomeGrowth;

    /**
     * YoY 매출 성장률 (Year over Year Revenue Growth)
     * 전년 동기 대비 매출 성장률 (%).
     */
    @Column(name = "yoy_revenue_growth")
    private BigDecimal yoyRevenueGrowth;

    /**
     * YoY 영업이익 성장률 (Year over Year Operating Income Growth)
     * 전년 동기 대비 영업이익 성장률 (%).
     */
    @Column(name = "yoy_op_income_growth")
    private BigDecimal yoyOpIncomeGrowth;

    /**
     * YoY 순이익 성장률 (Year over Year Net Income Growth)
     * 전년 동기 대비 순이익 성장률 (%).
     */
    @Column(name = "yoy_net_income_growth")
    private BigDecimal yoyNetIncomeGrowth;

}
