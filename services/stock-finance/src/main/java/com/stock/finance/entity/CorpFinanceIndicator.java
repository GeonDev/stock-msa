package com.stock.finance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@IdClass(CorpFinanceId.class)
@Table(name = "TB_CORP_FINANCE_INDICATOR")
public class CorpFinanceIndicator {

    @Id
    @Column(name = "CORP_CODE")
    private String corpCode;

    @Id
    @Column(name = "BAS_DT")
    private java.time.LocalDate basDt;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumns(value = {
            @JoinColumn(name = "CORP_CODE", referencedColumnName = "corpCode"),
            @JoinColumn(name = "BAS_DT", referencedColumnName = "basDt")
    }, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private CorpFinance corpFinance;

    /**
     * 주가수익비율 (Price-to-Earnings Ratio)
     * 주가를 주당순이익(EPS)으로 나눈 값. 낮을수록 저평가.
     */
    private Double per;

    /**
     * 주가순자산비율 (Price-to-Book-Value Ratio)
     * 주가를 주당순자산가치(BPS)로 나눈 값. 낮을수록 저평가.
     */
    private Double pbr;

    /**
     * 주가매출비율 (Price-to-Sales Ratio)
     * 주가를 주당매출액(SPS)으로 나눈 값. 성장주 가치 평가에 사용.
     */
    private Double psr;

    /**
     * 매출액 증가율 (Revenue Growth Rate)
     * 전년 대비 매출액의 성장률.
     */
    private Double revenueGrowth;

    /**
     * 순이익 증가율 (Net Income Growth Rate)
     * 전년 대비 순이익의 성장률.
     */
    private Double netIncomeGrowth;

    /**
     * 영업이익 증가율 (Operating Income Growth Rate)
     * 전년 대비 영업이익의 성장률.
     */
    private Double opIncomeGrowth;


    /**
     * 자기자본이익률 (Return on Equity)
     * 순이익을 자기자본으로 나눈 값. 높을수록 수익성이 좋음.
     */
    private Double roe;

    /**
     * 총자산이익률 (Return on Assets)
     * 순이익을 총자산으로 나눈 값. 높을수록 자산 활용 효율이 좋음.
     */
    private Double roa;

}
