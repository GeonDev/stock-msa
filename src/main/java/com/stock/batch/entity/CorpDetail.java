package com.stock.batch.entity;


import com.stock.batch.enums.CorpCurrency;
import com.stock.batch.enums.CorpNational;
import com.stock.batch.enums.CorpState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;


@Data
@Entity
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@Table(name = "TB_CORP_DETAIL")
public class CorpDetail implements Serializable {

    @Id
    String corpCode;

    @Enumerated(EnumType.STRING)
    CorpNational national;

    @Enumerated(EnumType.STRING)
    CorpState state;

    @Enumerated(EnumType.STRING)
    CorpCurrency corpType;

    //종가 - (n일 종가)의 +/- 값
    Integer momentum;

    @PrePersist
    public void prePersist() {
        this.momentum = (this.momentum == null ? 0 : this.momentum);
    }

}
