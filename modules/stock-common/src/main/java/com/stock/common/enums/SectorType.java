package com.stock.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SectorType {
    IT_HARDWARE("IT 하드웨어/반도체"),
    IT_SOFTWARE("IT 소프트웨어/서비스"),
    FINANCIALS("금융/지주사"),
    HEALTHCARE("보건의료/바이오"),
    CONSUMER_STAPLES("필수소비재"),
    CONSUMER_DISCRETIONARY("경기소비재"),
    INDUSTRIALS("산업재"),
    MATERIALS("소재/원자재"),
    ENERGY("에너지"),
    UTILITIES("유틸리티"),
    COMMUNICATIONS("통신/미디어"),
    ETC("기타");

    private final String description;

    public static SectorType fromDescription(String description) {
        for (SectorType type : values()) {
            if (type.description.equals(description) || description.contains(type.description)) {
                return type;
            }
        }
        return ETC;
    }
}
