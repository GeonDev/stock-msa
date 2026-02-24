package com.stock.strategy.service.slippage;

import com.stock.strategy.enums.OrderType;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FixedSlippageModel implements SlippageModel {

    private final BigDecimal slipRate;

    public FixedSlippageModel(BigDecimal slipRate) {
        this.slipRate = slipRate != null ? slipRate : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateExecutionPrice(BigDecimal originalPrice, int quantity, OrderType orderType) {
        BigDecimal slippageAmount = originalPrice.multiply(slipRate);
        
        // 매수할 때는 비싸게 사고, 매도할 때는 싸게 판다.
        if (orderType == OrderType.BUY) {
            return originalPrice.add(slippageAmount).setScale(2, RoundingMode.HALF_UP);
        } else {
            return originalPrice.subtract(slippageAmount).setScale(2, RoundingMode.HALF_UP);
        }
    }
}
