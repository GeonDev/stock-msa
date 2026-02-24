package com.stock.strategy.service.slippage;

import com.stock.strategy.enums.OrderType;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class VolumeBasedSlippageModel implements SlippageModel {

    private final BigDecimal baseSlipRate;
    private final BigDecimal penaltyRate;

    public VolumeBasedSlippageModel(BigDecimal baseSlipRate, BigDecimal penaltyRate) {
        this.baseSlipRate = baseSlipRate != null ? baseSlipRate : new BigDecimal("0.001");
        this.penaltyRate = penaltyRate != null ? penaltyRate : new BigDecimal("0.005");
    }

    @Override
    public BigDecimal calculateExecutionPrice(BigDecimal originalPrice, int quantity, OrderType orderType) {
        // 실제 구현에서는 일일 거래량 데이터를 받아와서 비율을 계산해야 하지만,
        // 여기서는 임의의 임계값(예: 주문량 1000주 이상)을 초과하면 페널티를 부과하는 방식으로 시뮬레이션
        BigDecimal currentSlipRate = baseSlipRate;
        if (quantity > 1000) {
            currentSlipRate = currentSlipRate.add(penaltyRate);
        }

        BigDecimal slippageAmount = originalPrice.multiply(currentSlipRate);

        if (orderType == OrderType.BUY) {
            return originalPrice.add(slippageAmount).setScale(2, RoundingMode.HALF_UP);
        } else {
            return originalPrice.subtract(slippageAmount).setScale(2, RoundingMode.HALF_UP);
        }
    }
}
