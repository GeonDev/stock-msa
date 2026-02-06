package com.stock.strategy.service;

import com.stock.strategy.strategy.EqualWeightStrategy;
import com.stock.strategy.strategy.Strategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StrategyFactory {

    private final EqualWeightStrategy equalWeightStrategy;

    public Strategy getStrategy(String strategyName) {
        return switch (strategyName.toLowerCase()) {
            case "equalweight" -> equalWeightStrategy;
            default -> throw new IllegalArgumentException("Unknown strategy: " + strategyName);
        };
    }
}
