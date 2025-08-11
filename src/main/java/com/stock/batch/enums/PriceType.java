package com.stock.batch.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PriceType {
    DAY5(5),
    DAY20(20),
    DAY60(60),
    DAY120(120),
    DAY200(200);

    private int value;

}
