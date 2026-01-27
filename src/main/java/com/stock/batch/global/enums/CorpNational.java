package com.stock.batch.global.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CorpNational {

    KR("대한민국"),
    JP("일본"),
    CN("중국"),
    HK("홍콩"),
    US("미국");

    private String desc;
}
