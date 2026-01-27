package com.stock.batch.global.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CorpCurrency {
    BANK("은행주"),
    DIVIDEND("배당주"),
    HOLDING("지주사"),
    ETC("기타");

    private String desc;

}
