package com.stock.strategy.strategy;

import com.stock.common.dto.CorpFinanceIndicatorDto;
import com.stock.common.dto.MultiFactorConfig;
import com.stock.common.dto.StockIndicatorDto;
import com.stock.common.utils.DateUtils;
import com.stock.strategy.client.FinanceClient;
import com.stock.strategy.client.PriceClient;
import com.stock.strategy.dto.BacktestRequest;
import com.stock.strategy.dto.PortfolioHolding;
import com.stock.strategy.dto.TradeOrder;
import com.stock.strategy.entity.FactorScore;
import com.stock.strategy.enums.OrderType;
import com.stock.strategy.service.FactorScoringService;
import com.stock.strategy.service.SimulationEngine.Portfolio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MultiFactorStrategy implements Strategy {

    private final PriceClient priceClient;
    private final FinanceClient financeClient;
    private final FactorScoringService factorScoringService;

    @Override
    public String getName() {
        return "MultiFactor";
    }

    @Override
    public List<TradeOrder> rebalance(LocalDate date, Portfolio portfolio, List<String> universe, BacktestRequest request) {
        MultiFactorConfig config = request.getMultiFactorConfig();
        if (config == null) {
            config = MultiFactorConfig.builder()
                    .topN(20)
                    .valueWeight(new BigDecimal("0.4"))
                    .momentumWeight(new BigDecimal("0.3"))
                    .qualityWeight(new BigDecimal("0.3"))
                    .build();
        }
        return rebalanceInternal(date, portfolio, universe, config);
    }

    private List<TradeOrder> rebalanceInternal(LocalDate date, Portfolio portfolio, List<String> universe, MultiFactorConfig config) {
        List<TradeOrder> orders = new ArrayList<>();
        if (universe.isEmpty()) return orders;

        String dateStr = DateUtils.toLocalDateString(date);

        try {
            List<CorpFinanceIndicatorDto> finances = financeClient.getIndicatorsBatch(universe, dateStr);
            List<StockIndicatorDto> indicators = priceClient.getIndicatorsByDateBatch(universe, dateStr);

            log.info("MultiFactor rebalance on {}: Universe={}, Finances={}, Indicators={}", 
                    date, universe.size(), finances.size(), indicators.size());

            List<FactorScore> scores = factorScoringService.calculateAndSaveScores(
                    date, finances, indicators, 
                    config.getValueWeight(), 
                    config.getMomentumWeight(), 
                    config.getQualityWeight()
            );

            if (scores.isEmpty()) return orders;

            // 상위 N개 선정
            List<String> targetStocks = scores.stream()
                    .sorted(Comparator.comparing(FactorScore::getTotalScore).reversed())
                    .limit(config.getTopN())
                    .map(FactorScore::getStockCode)
                    .collect(Collectors.toList());

            log.info("Selected {} multi-factor stocks for date {}", targetStocks.size(), date);

            // 1. 매도 (targetStocks에 없는 종목)
            for (String stockCode : new ArrayList<>(portfolio.getHoldings().keySet())) {
                if (!targetStocks.contains(stockCode)) {
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

            // 2. 매수 (동일 비중 가정)
            BigDecimal totalValue = portfolio.getTotalValue();
            BigDecimal targetValuePerStock = totalValue.divide(BigDecimal.valueOf(targetStocks.size()), 2, RoundingMode.HALF_UP);

            for (String stockCode : targetStocks) {
                var priceDto = priceClient.getPriceByDate(stockCode.startsWith("A") ? stockCode.substring(1) : stockCode, dateStr);
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
        } catch (Exception e) {
            log.error("Failed to rebalance MultiFactor", e);
        }

        return orders;
    }
}
