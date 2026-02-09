package com.stock.strategy.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StrategyType {
    EQUAL_WEIGHT("EqualWeight", "동일 비중 전략"),
    MOMENTUM("Momentum", "모멘텀 전략"),
    LOW_VOLATILITY("LowVolatility", "저변동성 전략"),
    VALUE("Value", "가치 투자 전략");

    private final String code;
    private final String description;

    public static StrategyType fromCode(String code) {
        for (StrategyType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown strategy code: " + code);
    }
}
