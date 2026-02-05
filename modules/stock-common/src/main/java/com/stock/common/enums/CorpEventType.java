package com.stock.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CorpEventType {
    PAID_INCREASE("유상증자"),
    FREE_INCREASE("무상증자"),
    CAPITAL_REDUCTION("감자"),
    STOCK_SPLIT("주식분할"),
    STOCK_MERGER("주식합병"),
    DIVIDEND("배당");

    private final String description;
}
