package com.stock.strategy.strategy;

import com.stock.strategy.dto.TradeOrder;
import com.stock.strategy.service.SimulationEngine.Portfolio;

import java.time.LocalDate;
import java.util.List;

public interface Strategy {
    String getName();
    List<TradeOrder> rebalance(LocalDate date, Portfolio portfolio, List<String> universe);
}
