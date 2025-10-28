package com.stock.batch.entity;


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
@Table(name = "TB_CORP_FINANCE")
public class CorpFinance implements Serializable {

    @Id
    String corpCode;

    //거래 기준 일자(년월일)
    LocalDate basDt;

    //법인세의 과세기간
    String bizYear;

    //통화 코드
    String currency;

    //영업이익(enpBzopPft)
    Integer opIncome;

    //투자금 (enpCptlAmt)
    Integer investment;

    //당기 순이익(enpCrtmNpf)
    Integer netIncome;

    //매출액 (enpSaleAmt)
    Integer revenue;

    //기업 총 자산 (enpTastAmt)
    Integer totalAsset;

    //기업 총 부채 (enpTdbtAmt)
    Integer totalDebt;

    //기업 총 자본 (enpTcptAmt)
    Integer totalCapital;

    // 회계 보고서 코드 (fnclDcd)
    String docCode;

    //회계 보고서 명칭 (fnclDcdNm)
    String docName;

    //회계 보고서 상 부채 비율 (fnclDebtRto)
    Double docDebtRatio;

    //법인세차감전순이익 (iclsPalClcAmt)
    Integer incomeBeforeTax;
}
