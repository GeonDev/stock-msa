package com.stock.strategy.strategy;

import com.stock.strategy.dto.BacktestRequest;
import com.stock.strategy.dto.TradeOrder;
import com.stock.strategy.service.SimulationEngine.Portfolio;

import java.time.LocalDate;
import java.util.List;

public interface Strategy {
    String getName();
    
    // 기본 구현 (하위 호환성용)
    default List<TradeOrder> rebalance(LocalDate date, Portfolio portfolio, List<String> universe) {
        return List.of();
    }

    // 설정 정보가 포함된 리밸런싱
    List<TradeOrder> rebalance(LocalDate date, Portfolio portfolio, List<String> universe, BacktestRequest request);
}
