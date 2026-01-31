package com.stock.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CorpState {
    ACTIVE("활성"),
    HALT("거래정지"),
    CAUTION("투자주의"),
    DEL("상장폐지");

    private String desc;

}
