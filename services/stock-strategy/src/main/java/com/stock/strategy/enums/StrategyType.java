package com.stock.strategy.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StrategyType {
    EQUAL_WEIGHT("EqualWeight", "동일 비중 전략"),
    MOMENTUM("Momentum", "모멘텀 전략"),
    LOW_VOLATILITY("LowVolatility", "저변동성 전략"),
    VALUE("Value", "가치 투자 전략"),
    MULTI_FACTOR("MultiFactor", "멀티팩터 스코어링 전략"),
    SECTOR_ROTATION("SectorRotation", "섹터 로테이션 전략"),
    DUAL_MOMENTUM("DualMomentum", "듀얼 모멘텀 자산배분 전략"),
    RISK_PARITY("RiskParity", "리스크 패리티 전략");

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
