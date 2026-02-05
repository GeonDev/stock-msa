package com.stock.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ValidationStatus {
    VERIFIED("검증 통과"),
    WARN_OUTLIER("비정상적 급등락 감지"),
    ERROR_IDENTITY("회계 항등식 불일치"),
    ERROR_MISSING("필수 데이터 누락");

    private final String description;
}
