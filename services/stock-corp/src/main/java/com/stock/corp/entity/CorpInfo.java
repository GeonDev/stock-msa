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
    String corpCode;

    String corpName;

    //주식식별번호(축약형)
    String stockCode;

    //국제 채권식별번호
    String isinCode;

    //검사일 -> 상장 폐지인지 확인
    @LastModifiedDate
    LocalDate checkDt;

}
