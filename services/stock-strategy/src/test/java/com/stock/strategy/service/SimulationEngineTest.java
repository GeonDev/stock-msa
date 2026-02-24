package com.stock.strategy.service;

import com.stock.strategy.dto.TradeOrder;
import com.stock.strategy.enums.OrderType;
import com.stock.strategy.repository.TradeHistoryRepository;
import com.stock.strategy.service.slippage.NoSlippageModel;
import com.stock.strategy.service.slippage.SlippageModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SimulationEngineTest {

    @Mock
    private TradeHistoryRepository tradeHistoryRepository;

    @InjectMocks
    private SimulationEngine engine;

    @Test
    @DisplayName("최대 비중 20% 제한 시 초과 매수 거부")
    void maxWeightLimitTest() {
        // given
        SimulationEngine.Portfolio portfolio = new SimulationEngine.Portfolio(new BigDecimal("10000000")); // 천만원
        BigDecimal feeRate = BigDecimal.ZERO;
        BigDecimal taxRate = BigDecimal.ZERO;
        SlippageModel slippageModel = new NoSlippageModel();
        BigDecimal maxWeightPerStock = new BigDecimal("0.2"); // 20%, 즉 최대 200만원

        // 300만원 어치 매수 시도 (300주 * 10000원)
        TradeOrder order = TradeOrder.builder()
                .stockCode("005930")
                .orderType(OrderType.BUY)
                .quantity(300)
                .price(new BigDecimal("10000"))
                .orderDate(LocalDate.now())
                .build();

        // when
        ReflectionTestUtils.invokeMethod(engine, "executeOrders",
                1L, LocalDate.now(), Collections.singletonList(order), portfolio, feeRate, taxRate, slippageModel, maxWeightPerStock);

        // then
        // 200만원 어치 (200주)만 매수되어야 함
        assertThat(portfolio.getHoldings().get("005930").getQuantity()).isEqualTo(200);
        assertThat(portfolio.getCashBalance()).isEqualByComparingTo(new BigDecimal("8000000")); // 잔고 800만원 남음
    }

    @Test
    @DisplayName("잔고 부족 시 가능한 수량만큼만 매수")
    void insufficientBalanceTest() {
        // given
        SimulationEngine.Portfolio portfolio = new SimulationEngine.Portfolio(new BigDecimal("1500000")); // 150만원
        BigDecimal feeRate = BigDecimal.ZERO;
        BigDecimal taxRate = BigDecimal.ZERO;
        SlippageModel slippageModel = new NoSlippageModel();
        BigDecimal maxWeightPerStock = new BigDecimal("1.0"); // 100%

        // 300만원 어치 매수 시도 (300주 * 10000원)
        TradeOrder order = TradeOrder.builder()
                .stockCode("005930")
                .orderType(OrderType.BUY)
                .quantity(300)
                .price(new BigDecimal("10000"))
                .orderDate(LocalDate.now())
                .build();

        // when
        ReflectionTestUtils.invokeMethod(engine, "executeOrders",
                1L, LocalDate.now(), Collections.singletonList(order), portfolio, feeRate, taxRate, slippageModel, maxWeightPerStock);

        // then
        // 150만원 어치 (150주)만 매수되어야 함
        assertThat(portfolio.getHoldings().get("005930").getQuantity()).isEqualTo(150);
        assertThat(portfolio.getCashBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
