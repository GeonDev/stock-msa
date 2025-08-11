package com.stock.batch.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CorpType {
    BANK("은행주"),
    DIVIDEND("배당주"),
    CHINA("중국주식"),
    JAPAN("일본주식"),
    USD("미국주식"),
    HOLDING("지주사"),
    ETC("기타");

    private String desc;

}
