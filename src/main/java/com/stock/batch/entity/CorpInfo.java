package com.stock.batch.entity;

import com.stock.batch.enums.CorpState;
import com.stock.batch.enums.CorpType;
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

    String stockCode;

    @Enumerated(EnumType.STRING)
    CorpState state;

    @Enumerated(EnumType.STRING)
    CorpType corpType;

    //기타 메모
    String message;

    //검사일 -> 상장 폐지인지 확인
    @LastModifiedDate
    LocalDate checkDt;

    //종가 - (n 개월전 종가)의 +/- 값, 7이상일때 상승
    Integer momentum;

    @PrePersist
    public void prePersist() {
        this.momentum = (this.momentum == null ? 0 : this.momentum);
    }

}
