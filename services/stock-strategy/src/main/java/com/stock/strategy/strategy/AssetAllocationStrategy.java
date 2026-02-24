package com.stock.strategy.strategy;

import com.stock.common.dto.AssetAllocationConfig;
import com.stock.common.dto.StockIndicatorDto;
import com.stock.common.utils.DateUtils;
import com.stock.strategy.client.PriceClient;
import com.stock.strategy.dto.PortfolioHolding;
import com.stock.strategy.dto.TradeOrder;
import com.stock.strategy.enums.OrderType;
import com.stock.strategy.service.SimulationEngine.Portfolio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssetAllocationStrategy implements Strategy {

    private final PriceClient priceClient;

    @Override
    public String getName() {
        return "AssetAllocation";
    }

    @Override
    public List<TradeOrder> rebalance(LocalDate date, Portfolio portfolio, List<String> universe) {
        throw new UnsupportedOperationException("AssetAllocationStrategy requires AssetAllocationConfig");
    }

    public List<TradeOrder> rebalance(LocalDate date, Portfolio portfolio, List<String> universe, AssetAllocationConfig config, com.stock.strategy.enums.StrategyType strategyType) {
        List<TradeOrder> orders = new ArrayList<>();
        if (universe.isEmpty()) return orders;

        String dateStr = DateUtils.toLocalDateString(date);

        try {
            // 1. Dual Momentum check (Absolute momentum of the market/universe)
            List<StockIndicatorDto> indicators = priceClient.getIndicatorsByDateBatch(universe, dateStr);
            
            double sumMom = 0;
            int count = 0;
            for (StockIndicatorDto ind : indicators) {
                if (ind.getMomentum6m() != null) {
                    sumMom += ind.getMomentum6m().doubleValue();
                    count++;
                }
            }

            double avgMomentum = count > 0 ? sumMom / count : 0.0;
            BigDecimal riskAssetWeight = config.getMaxRiskAssetWeight();

            // If dual momentum is enabled and average market momentum is negative, move to cash
            if (config.isUseDualMomentum() && avgMomentum < 0) {
                riskAssetWeight = BigDecimal.ZERO; // 100% Cash
                log.info("Dual Momentum triggered on {}: Market Momentum {} < 0. Moving to cash.", date, avgMomentum);
            }

            // Target Value for Risk Assets
            BigDecimal totalValue = portfolio.getTotalValue();
            BigDecimal targetRiskAssetValue = totalValue.multiply(riskAssetWeight).setScale(2, RoundingMode.HALF_UP);
            
            // Sell phase
            for (String stockCode : new ArrayList<>(portfolio.getHoldings().keySet())) {
                if (!universe.contains(stockCode) || targetRiskAssetValue.compareTo(BigDecimal.ZERO) == 0) {
                    PortfolioHolding holding = portfolio.getHoldings().get(stockCode);
                    orders.add(TradeOrder.builder()
                            .stockCode(stockCode)
                            .orderType(OrderType.SELL)
                            .quantity(holding.getQuantity())
                            .price(holding.getCurrentPrice())
                            .orderDate(date)
                            .build());
                }
            }

            // Buy/Adjust phase
            if (targetRiskAssetValue.compareTo(BigDecimal.ZERO) > 0) {
                Map<String, BigDecimal> targetWeights = new HashMap<>();

                if (strategyType == com.stock.strategy.enums.StrategyType.RISK_PARITY) {
                    // Simple Inverse Volatility
                    Map<String, Double> invVolMap = new HashMap<>();
                    double sumInvVol = 0.0;

                    for (StockIndicatorDto ind : indicators) {
                        if (ind.getStockCode() != null && ind.getBollingerUpper() != null && ind.getBollingerLower() != null) {
                            // proxy for volatility using bollinger band width
                            double bandWidth = ind.getBollingerUpper().doubleValue() - ind.getBollingerLower().doubleValue();
                            double price = ind.getMa20() != null ? ind.getMa20().doubleValue() : bandWidth;
                            double vol = price > 0 ? (bandWidth / price) : 0.01;
                            if (vol <= 0) vol = 0.01;

                            double invVol = 1.0 / vol;
                            invVolMap.put(ind.getStockCode(), invVol);
                            sumInvVol += invVol;
                        }
                    }

                    for (String stockCode : universe) {
                        double weight = sumInvVol > 0 && invVolMap.containsKey(stockCode) ? 
                                invVolMap.get(stockCode) / sumInvVol : 0.0;
                        targetWeights.put(stockCode, BigDecimal.valueOf(weight));
                    }
                } else {
                    // Equal weight
                    BigDecimal eqWeight = BigDecimal.ONE.divide(BigDecimal.valueOf(universe.size()), 4, RoundingMode.HALF_UP);
                    for (String stockCode : universe) {
                        targetWeights.put(stockCode, eqWeight);
                    }
                }

                for (String stockCode : universe) {
                    BigDecimal weight = targetWeights.getOrDefault(stockCode, BigDecimal.ZERO);
                    if (weight.compareTo(BigDecimal.ZERO) <= 0) continue;

                    BigDecimal targetValuePerStock = targetRiskAssetValue.multiply(weight).setScale(2, RoundingMode.HALF_UP);

                    var priceDto = priceClient.getPriceByDate(stockCode, dateStr);
                    if (priceDto == null || priceDto.getEndPrice() == null) continue;

                    BigDecimal currentPrice = priceDto.getEndPrice();
                    PortfolioHolding holding = portfolio.getHoldings().get(stockCode);
                    BigDecimal currentValue = holding != null ? holding.getMarketValue() : BigDecimal.ZERO;

                    BigDecimal diff = targetValuePerStock.subtract(currentValue);

                    if (diff.abs().compareTo(currentPrice) > 0) {
                        if (diff.compareTo(BigDecimal.ZERO) > 0) {
                            int quantity = diff.divide(currentPrice, 0, RoundingMode.DOWN).intValue();
                            if (quantity > 0) {
                                orders.add(TradeOrder.builder()
                                        .stockCode(stockCode)
                                        .orderType(OrderType.BUY)
                                        .quantity(quantity)
                                        .price(currentPrice)
                                        .orderDate(date)
                                        .build());
                            }
                        } else {
                            int quantity = diff.abs().divide(currentPrice, 0, RoundingMode.DOWN).intValue();
                            if (quantity > 0 && holding != null && holding.getQuantity() >= quantity) {
                                orders.add(TradeOrder.builder()
                                        .stockCode(stockCode)
                                        .orderType(OrderType.SELL)
                                        .quantity(quantity)
                                        .price(currentPrice)
                                        .orderDate(date)
                                        .build());
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to rebalance AssetAllocation", e);
        }

        return orders;
    }
}
