package com.stock.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SectorType {
    SEMICONDUCTORS("반도체"),
    IT_HARDWARE("IT 하드웨어"),
    IT_SOFTWARE("IT 소프트웨어"),
    FINANCIALS("금융/지주사"),
    HEALTHCARE("보건의료/바이오"),
    CONSUMER_STAPLES("필수소비재"),
    CONSUMER_DISCRETIONARY("경기소비재"),
    INDUSTRIALS("산업재"),
    MATERIALS("소재/원자재"),
    ENERGY("에너지"),
    UTILITIES("유틸리티"),
    COMMUNICATIONS("통신/미디어"),
    CONSTRUCTION("건설"),
    TRANSPORTATION("운송"),
    RETAIL("유통"),
    HOTELS_LEISURE("숙박/레저"),
    SERVICES("서비스"),
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

    public static SectorType fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return ETC;
        }

        // 3자리 상세 분류 우선 (예: 반도체)
        if (code.length() >= 3) {
            String prefix3 = code.substring(0, 3);
            if ("261".equals(prefix3)) return SEMICONDUCTORS;
        }

        // KSIC(한국표준산업분류) 중분류(앞 2자리) 기반 매핑
        String prefix2 = code.substring(0, Math.min(code.length(), 2));

        return switch (prefix2) {
            case "26" -> IT_HARDWARE;
            case "58", "62", "63" -> IT_SOFTWARE;
            case "64", "65", "66" -> FINANCIALS;
            case "21", "27" -> HEALTHCARE;
            case "10", "11" -> CONSUMER_STAPLES;
            case "13", "14", "15", "30" -> CONSUMER_DISCRETIONARY;
            case "25", "28", "29", "31", "33" -> INDUSTRIALS;
            case "20", "22", "23", "24" -> MATERIALS;
            case "05", "06", "19" -> ENERGY;
            case "35" -> UTILITIES;
            case "61" -> COMMUNICATIONS;
            case "41", "42" -> CONSTRUCTION;
            case "49", "50", "51", "52" -> TRANSPORTATION;
            case "47" -> RETAIL;
            case "55", "56", "91" -> HOTELS_LEISURE;
            case "70", "71", "72", "73", "74", "75" -> SERVICES;
            default -> ETC;
        };
    }
}
