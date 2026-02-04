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
    String corpCode;

    @Column(name = "corp_name")
    String corpName;

    //주식식별번호(축약형)
    @Column(name = "stock_code")
    String stockCode;

    //국제 채권식별번호
    @Column(name = "isin_code")
    String isinCode;

    //검사일 -> 상장 폐지인지 확인
    @LastModifiedDate
    @Column(name = "check_dt")
    LocalDate checkDt;

}
