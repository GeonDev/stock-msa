package com.stock.corp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.io.Serializable;
import java.time.LocalDate;


@Data
@Entity
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@Table(name = "TB_CORP_INFO")
public class CorpInfo implements Serializable {

    @Id
    @Column(name = "corp_code")
    private String corpCode;

    @Column(name = "corp_name")
    private String corpName;

    //주식식별번호(축약형)
    @Column(name = "stock_code")
    private String stockCode;

    //국제 채권식별번호
    @Column(name = "isin_code")
    private String isinCode;

    //DART 고유번호 (8자리)
    @Column(name = "dart_corp_code", length = 8)
    private String dartCorpCode;

    @Column(name = "market")
    private String market;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corp_code", insertable = false, updatable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private CorpDetail corpDetail;

    //검사일 -> 상장 폐지인지 확인
    @LastModifiedDate
    @Column(name = "check_dt")
    private LocalDate checkDt;

}
