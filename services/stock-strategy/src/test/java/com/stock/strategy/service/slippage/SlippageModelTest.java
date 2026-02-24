package com.stock.strategy.service.slippage;

import com.stock.strategy.enums.OrderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SlippageModelTest {

    @Test
    @DisplayName("고정 슬리피지 모델 - 매수 시 단가가 상승해야 한다")
    void fixedSlippageBuyTest() {
        // given
        FixedSlippageModel model = new FixedSlippageModel(new BigDecimal("0.002")); // 0.2%
        BigDecimal originalPrice = new BigDecimal("10000");

        // when
        BigDecimal execPrice = model.calculateExecutionPrice(originalPrice, 10, OrderType.BUY);

        // then
        // 10000 * 0.002 = 20 -> 10020
        assertThat(execPrice).isEqualByComparingTo(new BigDecimal("10020"));
    }

    @Test
    @DisplayName("고정 슬리피지 모델 - 매도 시 단가가 하락해야 한다")
    void fixedSlippageSellTest() {
        // given
        FixedSlippageModel model = new FixedSlippageModel(new BigDecimal("0.002")); // 0.2%
        BigDecimal originalPrice = new BigDecimal("10000");

        // when
        BigDecimal execPrice = model.calculateExecutionPrice(originalPrice, 10, OrderType.SELL);

        // then
        // 10000 * 0.002 = 20 -> 9980
        assertThat(execPrice).isEqualByComparingTo(new BigDecimal("9980"));
    }

    @Test
    @DisplayName("슬리피지 없음 모델 - 단가가 변하지 않아야 한다")
    void noSlippageTest() {
        // given
        NoSlippageModel model = new NoSlippageModel();
        BigDecimal originalPrice = new BigDecimal("10000");

        // when
        BigDecimal execPrice = model.calculateExecutionPrice(originalPrice, 10, OrderType.BUY);

        // then
        assertThat(execPrice).isEqualByComparingTo(new BigDecimal("10000"));
    }
}
