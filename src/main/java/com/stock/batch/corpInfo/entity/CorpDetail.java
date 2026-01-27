package com.stock.batch.corpInfo.entity;


import com.stock.batch.global.enums.CorpCurrency;
import com.stock.batch.global.enums.CorpNational;
import com.stock.batch.global.enums.CorpState;
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

}
