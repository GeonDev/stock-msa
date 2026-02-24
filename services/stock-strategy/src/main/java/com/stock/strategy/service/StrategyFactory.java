package com.stock.strategy.service;

import com.stock.strategy.enums.StrategyType;
import com.stock.strategy.strategy.EqualWeightStrategy;
import com.stock.strategy.strategy.LowVolatilityStrategy;
import com.stock.strategy.strategy.MomentumStrategy;
import com.stock.strategy.strategy.MultiFactorStrategy;
import com.stock.strategy.strategy.SectorRotationStrategy;
import com.stock.strategy.strategy.AssetAllocationStrategy;
import com.stock.strategy.strategy.Strategy;
import com.stock.strategy.strategy.ValueStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StrategyFactory {

    private final EqualWeightStrategy equalWeightStrategy;
    private final MomentumStrategy momentumStrategy;
    private final LowVolatilityStrategy lowVolatilityStrategy;
    private final ValueStrategy valueStrategy;
    private final MultiFactorStrategy multiFactorStrategy;
    private final SectorRotationStrategy sectorRotationStrategy;
    private final AssetAllocationStrategy assetAllocationStrategy;

    public Strategy getStrategy(StrategyType strategyType) {
        return switch (strategyType) {
            case EQUAL_WEIGHT -> equalWeightStrategy;
            case MOMENTUM -> momentumStrategy;
            case LOW_VOLATILITY -> lowVolatilityStrategy;
            case VALUE -> valueStrategy;
            case MULTI_FACTOR -> multiFactorStrategy;
            case SECTOR_ROTATION -> sectorRotationStrategy;
            case DUAL_MOMENTUM, RISK_PARITY -> assetAllocationStrategy;
        };
    }

    public Strategy getStrategy(String strategyName) {
        return getStrategy(StrategyType.fromCode(strategyName));
    }

    public List<StrategyType> getAvailableStrategies() {
        return Arrays.asList(StrategyType.values());
    }
}
