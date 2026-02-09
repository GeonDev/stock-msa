package com.stock.strategy.strategy;

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
public class MomentumStrategy implements Strategy {

    private final PriceClient priceClient;
    
    private static final int TOP_N = 20;
    private static final int MOMENTUM_1M_DAYS = 20;
    private static final int MOMENTUM_3M_DAYS = 60;
    private static final int MOMENTUM_6M_DAYS = 120;
    
    private static final BigDecimal WEIGHT_1M = new BigDecimal("0.5");
    private static final BigDecimal WEIGHT_3M = new BigDecimal("0.3");
    private static final BigDecimal WEIGHT_6M = new BigDecimal("0.2");

    @Override
    public String getName() {
        return "Momentum";
    }

    @Override
    public List<TradeOrder> rebalance(LocalDate date, Portfolio portfolio, List<String> universe) {
        List<TradeOrder> orders = new ArrayList<>();

        if (universe.isEmpty()) {
            return orders;
        }

        try {
            // 1. 모멘텀 스코어 계산
            Map<String, BigDecimal> momentumScores = calculateMomentumScores(universe, date);
            
            // 2. 상위 N개 종목 선정
            List<String> topStocks = momentumScores.entrySet().stream()
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                    .limit(TOP_N)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (topStocks.isEmpty()) {
                return orders;
            }

            // 3. 전체 자산 가치
            BigDecimal totalValue = portfolio.getTotalValue();
            
            // 4. 종목당 목표 금액 (동일 비중)
            BigDecimal targetValuePerStock = totalValue.divide(
                    BigDecimal.valueOf(topStocks.size()), 2, RoundingMode.HALF_UP);

            // 5. 기존 보유 종목 중 상위 N개에 없는 종목 매도
            for (String stockCode : new ArrayList<>(portfolio.getHoldings().keySet())) {
                if (!topStocks.contains(stockCode)) {
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

            // 6. 상위 종목 리밸런싱
            String dateStr = DateUtils.toLocalDateString(date);
            for (String stockCode : topStocks) {
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
                            // 매수
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
                            // 매도
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
            log.error("Failed to rebalance with momentum strategy", e);
        }

        return orders;
    }

    private Map<String, BigDecimal> calculateMomentumScores(List<String> universe, LocalDate date) {
        Map<String, BigDecimal> scores = new HashMap<>();
        
        LocalDate startDate = date.minusDays(MOMENTUM_6M_DAYS + 10);
        String startDateStr = DateUtils.toLocalDateString(startDate);
        String endDateStr = DateUtils.toLocalDateString(date);

        for (String stockCode : universe) {
            try {
                var priceHistory = priceClient.getPriceHistory(stockCode, startDateStr, endDateStr);
                
                if (priceHistory == null || priceHistory.isEmpty()) {
                    continue;
                }

                // 날짜순 정렬
                priceHistory.sort(Comparator.comparing(p -> p.getBasDt()));

                BigDecimal momentum1m = calculateMomentum(priceHistory, MOMENTUM_1M_DAYS);
                BigDecimal momentum3m = calculateMomentum(priceHistory, MOMENTUM_3M_DAYS);
                BigDecimal momentum6m = calculateMomentum(priceHistory, MOMENTUM_6M_DAYS);

                if (momentum1m != null && momentum3m != null && momentum6m != null) {
                    BigDecimal score = momentum1m.multiply(WEIGHT_1M)
                            .add(momentum3m.multiply(WEIGHT_3M))
                            .add(momentum6m.multiply(WEIGHT_6M));
                    scores.put(stockCode, score);
                }
            } catch (Exception e) {
                log.warn("Failed to calculate momentum for {}: {}", stockCode, e.getMessage());
            }
        }

        return scores;
    }

    private BigDecimal calculateMomentum(List<com.stock.common.dto.StockPriceDto> priceHistory, int days) {
        if (priceHistory.size() < days + 1) {
            return null;
        }

        int endIndex = priceHistory.size() - 1;
        int startIndex = endIndex - days;

        if (startIndex < 0) {
            return null;
        }

        BigDecimal endPrice = priceHistory.get(endIndex).getEndPrice();
        BigDecimal startPrice = priceHistory.get(startIndex).getEndPrice();

        if (startPrice == null || endPrice == null || startPrice.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return endPrice.subtract(startPrice)
                .divide(startPrice, 8, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}
