package com.stock.batch.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StockCategory {
    NONE("일반"),
    BANK("은행주"),
    DIVIDEND("배당주"),
    HOLDING("지주사"),
    ETC("기타");

    private String desc;
}
