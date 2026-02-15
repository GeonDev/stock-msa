package com.stock.strategy.strategy;

import com.stock.common.dto.CorpFinanceIndicatorDto;
import com.stock.common.dto.ValueStrategyConfig;
import com.stock.common.utils.DateUtils;
import com.stock.strategy.client.FinanceClient;
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
public class ValueStrategy implements Strategy {

    private final PriceClient priceClient;
    private final FinanceClient financeClient;
    
    // 기본값
    private static final int DEFAULT_TOP_N = 20;
    private static final BigDecimal DEFAULT_WEIGHT_PER = new BigDecimal("0.3");
    private static final BigDecimal DEFAULT_WEIGHT_PBR = new BigDecimal("0.3");
    private static final BigDecimal DEFAULT_WEIGHT_ROE = new BigDecimal("0.4");

    @Override
    public String getName() {
        return "Value";
    }

    @Override
    public List<TradeOrder> rebalance(LocalDate date, Portfolio portfolio, List<String> universe) {
        return rebalance(date, portfolio, universe, null);
    }
    
    /**
     * 가중치 설정 가능한 리밸런싱
     */
    public List<TradeOrder> rebalance(LocalDate date, Portfolio portfolio, 
                                      List<String> universe, ValueStrategyConfig config) {
        List<TradeOrder> orders = new ArrayList<>();

        if (universe.isEmpty()) {
            return orders;
        }

        // 설정값 또는 기본값 사용
        if (config == null) {
            config = ValueStrategyConfig.builder().build();
        }
        config.validate();
        
        int topN = config.getTopN();
        BigDecimal perWeight = config.getPerWeight();
        BigDecimal pbrWeight = config.getPbrWeight();
        BigDecimal roeWeight = config.getRoeWeight();

        try {
            // 1. 가치 스코어 계산
            Map<String, BigDecimal> valueScores = 
                    calculateValueScores(universe, date, perWeight, pbrWeight, roeWeight);
            
            if (valueScores.isEmpty()) {
                log.warn("No valid value scores calculated for date: {}", date);
                return orders;
            }

            // 2. 상위 N개 종목 선정
            List<String> topStocks = valueScores.entrySet().stream()
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                    .limit(topN)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (topStocks.isEmpty()) {
                return orders;
            }

            log.info("Selected {} value stocks for date {}", topStocks.size(), date);

            // 3. 전체 자산 가치
            BigDecimal totalValue = portfolio.getTotalValue();
            BigDecimal targetValuePerStock = totalValue.divide(
                    BigDecimal.valueOf(topStocks.size()), 2, RoundingMode.HALF_UP);

            // 4. 기존 보유 종목 중 상위 N개에 없는 종목 매도
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

            // 5. 상위 종목 리밸런싱
            String dateStr = DateUtils.toLocalDateString(date);
            for (String stockCode : topStocks) {
                try {
                    var priceDto = priceClient.getPriceByDate(stockCode, dateStr);
                    if (priceDto == null || priceDto.getEndPrice() == null) {
                        continue;
                    }

                    BigDecimal currentPrice = priceDto.getEndPrice();
                    PortfolioHolding holding = portfolio.getHoldings().get(stockCode);
                    BigDecimal currentValue = holding != null ? holding.getMarketValue() : BigDecimal.ZERO;
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
            log.error("Failed to rebalance with value strategy", e);
        }

        return orders;
    }

    private Map<String, BigDecimal> calculateValueScores(List<String> universe, LocalDate date,
                                                         BigDecimal perWeight, BigDecimal pbrWeight, 
                                                         BigDecimal roeWeight) {
        Map<String, BigDecimal> scores = new HashMap<>();
        String dateStr = date.toString();
        
        try {
            List<CorpFinanceIndicatorDto> indicators = financeClient.getIndicatorsBatch(universe, dateStr);
            
            log.info("Fetched {} indicators for {} stocks on {}", indicators.size(), universe.size(), date);
            
            for (CorpFinanceIndicatorDto indicator : indicators) {
                try {
                    BigDecimal score = calculateScore(indicator, perWeight, pbrWeight, roeWeight);
                    if (score != null) {
                        scores.put(indicator.getCorpCode(), score);
                    }
                } catch (Exception e) {
                    log.warn("Failed to calculate value score for {}: {}", 
                            indicator.getCorpCode(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch indicators batch", e);
        }

        return scores;
    }

    private BigDecimal calculateScore(CorpFinanceIndicatorDto indicator,
                                     BigDecimal perWeight, BigDecimal pbrWeight, BigDecimal roeWeight) {
        BigDecimal per = indicator.getPer();
        BigDecimal pbr = indicator.getPbr();
        BigDecimal roe = indicator.getRoe();

        // 필수 지표 검증
        if (per == null || pbr == null || roe == null) {
            return null;
        }

        // 음수/0 제외
        if (per.compareTo(BigDecimal.ZERO) <= 0 || 
            pbr.compareTo(BigDecimal.ZERO) <= 0 || 
            roe.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        // 가치 스코어 = (1/PER) * perWeight + (1/PBR) * pbrWeight + (ROE/100) * roeWeight
        BigDecimal perScore = BigDecimal.ONE.divide(per, 8, RoundingMode.HALF_UP).multiply(perWeight);
        BigDecimal pbrScore = BigDecimal.ONE.divide(pbr, 8, RoundingMode.HALF_UP).multiply(pbrWeight);
        BigDecimal roeScore = roe.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP).multiply(roeWeight);

        return perScore.add(pbrScore).add(roeScore);
    }
}
