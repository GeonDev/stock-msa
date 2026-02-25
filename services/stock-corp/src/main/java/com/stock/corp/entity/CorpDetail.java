package com.stock.corp.entity;


import com.stock.common.enums.CorpCurrency;
import com.stock.common.enums.CorpNational;
import com.stock.common.enums.CorpState;
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
    @Column(name = "corp_code")
    private String corpCode;

    @Enumerated(EnumType.STRING)
    private CorpNational national;

    @Enumerated(EnumType.STRING)
    private CorpState state;

    @Enumerated(EnumType.STRING)
    @Column(name = "corp_type")
    private CorpCurrency corpType;

    @Enumerated(EnumType.STRING)
    @Column(name = "sector")
    private com.stock.common.enums.SectorType sector;

}
