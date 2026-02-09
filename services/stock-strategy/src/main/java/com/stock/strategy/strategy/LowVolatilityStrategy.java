package com.stock.strategy.strategy;

import com.stock.common.dto.StockPriceDto;
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
public class LowVolatilityStrategy implements Strategy {

    private final PriceClient priceClient;
    
    private static final int TOP_N = 20;
    private static final int VOLATILITY_PERIOD = 60;

    @Override
    public String getName() {
        return "LowVolatility";
    }

    @Override
    public List<TradeOrder> rebalance(LocalDate date, Portfolio portfolio, List<String> universe) {
        List<TradeOrder> orders = new ArrayList<>();

        if (universe.isEmpty()) {
            return orders;
        }

        try {
            Map<String, BigDecimal> volatilityScores = calculateVolatility(universe, date);
            
            List<String> lowVolStocks = volatilityScores.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .limit(TOP_N)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (lowVolStocks.isEmpty()) {
                return orders;
            }

            BigDecimal totalValue = portfolio.getTotalValue();
            BigDecimal targetValuePerStock = totalValue.divide(
                    BigDecimal.valueOf(lowVolStocks.size()), 2, RoundingMode.HALF_UP);

            for (String stockCode : new ArrayList<>(portfolio.getHoldings().keySet())) {
                if (!lowVolStocks.contains(stockCode)) {
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

            String dateStr = DateUtils.toLocalDateString(date);
            for (String stockCode : lowVolStocks) {
                try {
                    var priceDto = priceClient.getPriceByDate(stockCode, dateStr);
                    if (priceDto == null || priceDto.getEndPrice() == null) {
                        continue;
                    }

                    BigDecimal currentPrice = priceDto.getEndPrice();
                    PortfolioHolding holding = portfolio.getHoldings().get(stockCode);
                    
                    BigDecimal currentValue = holding != null 
                            ? holding.getMarketValue() 
                            : BigDecimal.ZERO;

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
                } catch (Exception e) {
                    log.warn("Failed to get price for {}: {}", stockCode, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Failed to rebalance with low volatility strategy", e);
        }

        return orders;
    }

    private Map<String, BigDecimal> calculateVolatility(List<String> universe, LocalDate date) {
        Map<String, BigDecimal> volatilities = new HashMap<>();
        
        LocalDate startDate = date.minusDays(VOLATILITY_PERIOD + 10);
        String startDateStr = DateUtils.toLocalDateString(startDate);
        String endDateStr = DateUtils.toLocalDateString(date);

        for (String stockCode : universe) {
            try {
                var priceHistory = priceClient.getPriceHistory(stockCode, startDateStr, endDateStr);
                
                if (priceHistory == null || priceHistory.size() < VOLATILITY_PERIOD) {
                    continue;
                }

                priceHistory.sort(Comparator.comparing(StockPriceDto::getBasDt));

                List<BigDecimal> returns = new ArrayList<>();
                for (int i = 1; i < priceHistory.size(); i++) {
                    BigDecimal prevPrice = priceHistory.get(i - 1).getEndPrice();
                    BigDecimal currPrice = priceHistory.get(i).getEndPrice();
                    
                    if (prevPrice != null && currPrice != null && prevPrice.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal dailyReturn = currPrice.subtract(prevPrice)
                                .divide(prevPrice, 8, RoundingMode.HALF_UP);
                        returns.add(dailyReturn);
                    }
                }

                if (returns.size() >= VOLATILITY_PERIOD - 1) {
                    BigDecimal volatility = calculateStandardDeviation(returns);
                    volatilities.put(stockCode, volatility);
                }
            } catch (Exception e) {
                log.warn("Failed to calculate volatility for {}: {}", stockCode, e.getMessage());
            }
        }

        return volatilities;
    }

    private BigDecimal calculateStandardDeviation(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mean = sum.divide(BigDecimal.valueOf(values.size()), 8, RoundingMode.HALF_UP);

        BigDecimal variance = values.stream()
                .map(v -> v.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 8, RoundingMode.HALF_UP);

        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
    }
}
