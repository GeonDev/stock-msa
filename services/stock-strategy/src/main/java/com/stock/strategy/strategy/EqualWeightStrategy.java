package com.stock.strategy.strategy;

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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EqualWeightStrategy implements Strategy {

    private final PriceClient priceClient;

    @Override
    public String getName() {
        return "EqualWeight";
    }

    @Override
    public List<TradeOrder> rebalance(LocalDate date, Portfolio portfolio, List<String> universe) {
        List<TradeOrder> orders = new ArrayList<>();

        if (universe.isEmpty()) {
            return orders;
        }

        try {
            // 전체 자산 가치
            BigDecimal totalValue = portfolio.getTotalValue();
            
            // 종목당 목표 금액 (동일 비중)
            BigDecimal targetValuePerStock = totalValue.divide(
                    BigDecimal.valueOf(universe.size()), 2, RoundingMode.HALF_UP);

            // 기존 보유 종목 중 유니버스에 없는 종목 매도
            for (String stockCode : new ArrayList<>(portfolio.getHoldings().keySet())) {
                if (!universe.contains(stockCode)) {
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

            // 유니버스 종목 리밸런싱
            for (String stockCode : universe) {
                try {
                    var priceDto = priceClient.getPriceByDate(stockCode, date.toString());
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
            log.error("Failed to rebalance", e);
        }

        return orders;
    }
}
