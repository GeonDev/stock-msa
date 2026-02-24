package com.stock.strategy.service.slippage;

import com.stock.strategy.enums.OrderType;

import java.math.BigDecimal;

public interface SlippageModel {
    /**
     * 슬리피지가 반영된 실제 체결 단가를 계산합니다.
     *
     * @param originalPrice 원래 시장가
     * @param quantity      주문 수량
     * @param orderType     주문 종류(매수/매도)
     * @return 슬리피지가 적용된 체결가
     */
    BigDecimal calculateExecutionPrice(BigDecimal originalPrice, int quantity, OrderType orderType);
}
