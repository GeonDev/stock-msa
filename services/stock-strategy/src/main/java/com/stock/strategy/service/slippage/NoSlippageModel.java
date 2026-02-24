package com.stock.strategy.service.slippage;

import com.stock.strategy.enums.OrderType;

import java.math.BigDecimal;

public class NoSlippageModel implements SlippageModel {

    @Override
    public BigDecimal calculateExecutionPrice(BigDecimal originalPrice, int quantity, OrderType orderType) {
        return originalPrice;
    }
}
