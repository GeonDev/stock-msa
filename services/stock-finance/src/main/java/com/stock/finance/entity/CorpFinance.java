package com.stock.finance.entity;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
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
    String corpCode;

    @Id
    @Schema(description = "거래 기준 일자", example = "2024-01-01")
    LocalDate basDt;

    @Schema(description = "법인세의 과세기간 (사업연도)", example = "2023")
    String bizYear;

    @Schema(description = "통화 코드", example = "KRW")
    String currency;

    @Schema(description = "당기 영업이익")
    Long opIncome;

    @Schema(description = "전기 영업이익")
    Long prevOpIncome;

    @Schema(description = "투자금 (자본금)")
    Long investment;

    @Schema(description = "당기 순이익")
    Long netIncome;

    @Schema(description = "전기 순이익")
    Long prevNetIncome;

    @Schema(description = "당기 매출액")
    Long revenue;

    @Schema(description = "전기 매출액")
    Long prevRevenue;

    @Schema(description = "기업 총 자산")
    Long totalAsset;

    @Schema(description = "기업 총 부채")
    Long totalDebt;

    @Schema(description = "기업 총 자본")
    Long totalCapital;

    @Schema(description = "회계 보고서 코드 (11013: 1분기, 11012: 반기, 11014: 3분기, 11011: 사업보고서)")
    String docCode;

    @Schema(description = "회계 보고서 명칭")
    String docName;

    @Schema(description = "회계 보고서 상 부채 비율")
    Double docDebtRatio;

    @Schema(description = "법인세차감전순이익")
    Long incomeBeforeTax;

    @OneToOne(mappedBy = "corpFinance", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Schema(description = "재무 지표 정보")
    private CorpFinanceIndicator corpFinanceIndicator;
}
