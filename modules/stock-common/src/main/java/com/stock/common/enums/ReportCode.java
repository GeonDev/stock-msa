package com.stock.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Getter
@RequiredArgsConstructor
public enum ReportCode {
    Q1("11013", "1분기보고서", 3, 31),
    SEMI("11012", "반기보고서", 6, 30),
    Q3("11014", "3분기보고서", 9, 30),
    ANNUAL("11011", "사업보고서", 12, 31);

    private final String code;
    private final String description;
    private final int month;
    private final int day;

    /**
     * 보고서 코드로 Enum 조회
     */
    public static ReportCode fromCode(String code) {
        for (ReportCode reportCode : values()) {
            if (reportCode.code.equals(code)) {
                return reportCode;
            }
        }
        throw new IllegalArgumentException("Invalid report code: " + code);
    }

    /**
     * 기준일자 계산 (해당 분기 마지막 날)
     */
    public LocalDate getBasDt(int year) {
        return LocalDate.of(year, month, day);
    }

    /**
     * 이전 분기 조회
     */
    public ReportCode getPrevious() {
        return switch (this) {
            case Q1 -> null;  // 이전 분기 없음
            case SEMI -> Q1;
            case Q3 -> SEMI;
            case ANNUAL -> Q3;
        };
    }

    /**
     * 다음 분기 조회
     */
    public ReportCode getNext() {
        return switch (this) {
            case Q1 -> SEMI;
            case SEMI -> Q3;
            case Q3 -> ANNUAL;
            case ANNUAL -> null;  // 다음 분기 없음
        };
    }

    /**
     * 분기 순서 (1~4)
     */
    public int getQuarter() {
        return switch (this) {
            case Q1 -> 1;
            case SEMI -> 2;
            case Q3 -> 3;
            case ANNUAL -> 4;
        };
    }

    /**
     * 누적 데이터 여부
     */
    public boolean isCumulative() {
        return true;  // DART API는 모두 누적 데이터
    }
}
